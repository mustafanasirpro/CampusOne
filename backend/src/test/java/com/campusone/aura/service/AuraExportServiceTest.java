package com.campusone.aura.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.campusone.aura.dto.AuraDtos.SessionResponse;
import com.campusone.aura.dto.AuraDtos.TermResponse;
import com.campusone.aura.dto.AuraDtos.TimetableVersionResponse;
import com.campusone.aura.repository.AuraJdbcRepository;
import com.campusone.aura.repository.AuraJdbcRepository.ScopedResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.pdfbox.Loader;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuraExportServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID UNIVERSITY_ID = UUID.randomUUID();
    private static final UUID VERSION_ID = UUID.randomUUID();
    private static final UUID TERM_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();

    @Mock
    private AuraAuthorizationService authorizationService;

    @Mock
    private AuraJdbcRepository repository;

    private AuraExportService service;

    @BeforeEach
    void setUp() {
        service = new AuraExportService(
                authorizationService, repository,
                new ObjectMapper().findAndRegisterModules());
        when(authorizationService.requireAdminUniversity(USER_ID))
                .thenReturn(UNIVERSITY_ID);
        when(repository.findVersion(VERSION_ID)).thenReturn(Optional.of(
                new TimetableVersionResponse(
                        VERSION_ID, TERM_ID, null, 1, "DRAFT", "0hard/0soft",
                        null, Instant.parse("2026-01-01T00:00:00Z"), null)));
        when(repository.findTerm(TERM_ID)).thenReturn(Optional.of(
                new TermResponse(
                        TERM_ID, UNIVERSITY_ID, "FA26", "Fall 2026",
                        LocalDate.of(2026, 8, 24),
                        LocalDate.of(2026, 12, 18),
                        "DRAFT", "Asia/Karachi",
                        Instant.parse("2026-01-01T00:00:00Z"),
                        Instant.parse("2026-01-01T00:00:00Z"), 0)));
        when(repository.calendarCancellationDates(VERSION_ID))
                .thenReturn(Map.of());
    }

    @Test
    void exportCsv_usesScopedSessionsAndQuotesSpreadsheetValues() {
        when(repository.resourceBelongsToUniversity(
                ScopedResource.VERSION, VERSION_ID, UNIVERSITY_ID))
                .thenReturn(true);
        when(repository.listSessions(VERSION_ID)).thenReturn(List.of(session()));

        AuraExportService.ExportPayload result = service.export(
                USER_ID, VERSION_ID, "csv");

        String text = new String(result.bytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(result.filename()).isEqualTo("aura-timetable.csv");
        assertThat(result.contentType()).isEqualTo("text/csv");
        assertThat(text).contains("Course,Title,Section");
        assertThat(text).contains("\"CS101\",\"Programming, Fundamentals\"");
    }

    @Test
    void export_rejectsUnknownFormat() {
        when(repository.resourceBelongsToUniversity(
                ScopedResource.VERSION, VERSION_ID, UNIVERSITY_ID))
                .thenReturn(true);
        when(repository.listSessions(VERSION_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> service.export(USER_ID, VERSION_ID, "DOCX"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Choose CSV");
    }

    @Test
    void everySupportedFormatProducesStructurallyParseableOutput()
            throws Exception {
        when(repository.resourceBelongsToUniversity(
                ScopedResource.VERSION, VERSION_ID, UNIVERSITY_ID))
                .thenReturn(true);
        when(repository.listSessions(VERSION_ID)).thenReturn(List.of(session()));
        Map<String, AuraExportService.ExportPayload> exports = new HashMap<>();
        for (String format : List.of("CSV", "JSON", "XLSX", "HTML", "ICS", "PDF")) {
            exports.put(format, service.export(USER_ID, VERSION_ID, format));
        }

        try (CSVParser csv = CSVParser.parse(
                new String(exports.get("CSV").bytes(),
                        java.nio.charset.StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
                        .get())) {
            assertThat(csv.getRecords()).hasSize(1);
        }
        assertThat(new ObjectMapper().findAndRegisterModules()
                .readTree(exports.get("JSON").bytes()))
                .hasSize(1);
        try (XSSFWorkbook workbook = new XSSFWorkbook(
                new ByteArrayInputStream(exports.get("XLSX").bytes()))) {
            assertThat(workbook.getSheet("Timetable").getPhysicalNumberOfRows())
                    .isEqualTo(2);
        }
        final int[] tableCount = {0};
        new ParserDelegator().parse(
                new StringReader(new String(
                        exports.get("HTML").bytes(),
                        java.nio.charset.StandardCharsets.UTF_8)),
                new HTMLEditorKit.ParserCallback() {
                    @Override
                    public void handleStartTag(
                            HTML.Tag tag,
                            MutableAttributeSet attributes,
                            int position) {
                        if (HTML.Tag.TABLE.equals(tag)) tableCount[0]++;
                    }
                },
                true);
        assertThat(tableCount[0]).isEqualTo(1);
        String calendar = new String(
                exports.get("ICS").bytes(),
                java.nio.charset.StandardCharsets.UTF_8);
        assertThat(calendar).startsWith("BEGIN:VCALENDAR")
                .contains("BEGIN:VEVENT")
                .endsWith("END:VCALENDAR\r\n");
        try (var document = Loader.loadPDF(exports.get("PDF").bytes())) {
            assertThat(document.getNumberOfPages()).isPositive();
        }
    }

    @Test
    void csvNeutralizesSpreadsheetFormulaPrefixes() {
        when(repository.resourceBelongsToUniversity(
                ScopedResource.VERSION, VERSION_ID, UNIVERSITY_ID))
                .thenReturn(true);
        SessionResponse unsafe = new SessionResponse(
                UUID.randomUUID(), VERSION_ID, UUID.randomUUID(), UUID.randomUUID(),
                "=HYPERLINK(\"https://example.test\")", "Title",
                UUID.randomUUID(), "Section", UUID.randomUUID(), "Instructor",
                UUID.randomUUID(), "Room", "CLASSROOM", UUID.randomUUID(), 1,
                LocalTime.of(9, 0), LocalTime.of(10, 0), false, "SOLVER");
        when(repository.listSessions(VERSION_ID)).thenReturn(List.of(unsafe));

        String csv = new String(
                service.export(USER_ID, VERSION_ID, "CSV").bytes(),
                java.nio.charset.StandardCharsets.UTF_8);

        assertThat(csv).contains("\"'=HYPERLINK(\"\"https://example.test\"\")\"");
    }

    @Test
    void icsUsesDatedOccurrencesAndOmitsCancelledCalendarDates() {
        when(repository.resourceBelongsToUniversity(
                ScopedResource.VERSION, VERSION_ID, UNIVERSITY_ID))
                .thenReturn(true);
        when(repository.listSessions(VERSION_ID)).thenReturn(List.of(session()));
        when(repository.calendarCancellationDates(VERSION_ID)).thenReturn(Map.of(
                SESSION_ID, List.of(LocalDate.of(2026, 8, 24))));

        String calendar = new String(
                service.export(USER_ID, VERSION_ID, "ICS").bytes(),
                java.nio.charset.StandardCharsets.UTF_8);

        assertThat(calendar)
                .contains("DTSTART;TZID=Asia/Karachi:20260831T090000")
                .contains("RDATE;TZID=Asia/Karachi:")
                .doesNotContain("20260824T090000");
    }

    private SessionResponse session() {
        return new SessionResponse(
                SESSION_ID, VERSION_ID, UUID.randomUUID(), UUID.randomUUID(),
                "CS101", "Programming, Fundamentals", UUID.randomUUID(),
                "BSCS-1A", UUID.randomUUID(), "Dr Ahmed", UUID.randomUUID(),
                "Room 1", "CLASSROOM", UUID.randomUUID(), 1,
                LocalTime.of(9, 0), LocalTime.of(10, 0), false, "SOLVER");
    }
}
