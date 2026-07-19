package com.campusone.aura.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.aura.exception.AuraStateException;
import com.campusone.aura.repository.AuraImportRepository;
import com.campusone.aura.repository.AuraImportRepository.ImportJob;
import com.campusone.aura.repository.AuraImportRepository.SourceRow;
import com.campusone.aura.repository.AuraJdbcRepository;
import com.campusone.aura.repository.AuraJdbcRepository.ScopedResource;
import com.campusone.aura.dto.AuraImportDtos.ImportApplyResponse;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class AuraImportServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID UNIVERSITY_ID = UUID.randomUUID();
    private static final UUID TERM_ID = UUID.randomUUID();

    @Mock
    private AuraAuthorizationService authorizationService;
    @Mock
    private AuraJdbcRepository auraRepository;
    @Mock
    private AuraImportRepository importRepository;
    @Mock
    private AuraService auraService;
    @Mock
    private AuraRegistrationService registrationService;
    @Mock
    private AuraClashDetector clashDetector;

    private AuraImportService service;

    @BeforeEach
    void setUp() {
        service = new AuraImportService(
                authorizationService,
                auraRepository,
                importRepository,
                auraService,
                registrationService,
                clashDetector,
                Clock.fixed(Instant.parse("2026-07-15T00:00:00Z"), ZoneOffset.UTC),
                10,
                10_000);
    }

    @Test
    void previewCsv_suggestsTimetableMappingAndPersistsSafePreview() {
        stubScope();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "timetable.csv",
                "text/csv",
                "Day,Start Time,End Time,Course Code,Section,Teacher,Room\n"
                        .concat("Monday,09:00,10:00,CS101,A,Dr Khan,R1\n")
                        .getBytes(StandardCharsets.UTF_8));

        var response = service.preview(
                USER_ID, TERM_ID, "timetable", null, file);

        assertThat(response.fileFormat()).isEqualTo("CSV");
        assertThat(response.totalRows()).isEqualTo(1);
        assertThat(response.suggestedMapping())
                .containsEntry("DAY", "Day")
                .containsEntry("START", "Start Time")
                .containsEntry("COURSE", "Course Code")
                .containsEntry("INSTRUCTOR", "Teacher");
        verify(importRepository).insertPreview(
                any(),
                eq(UNIVERSITY_ID),
                eq(TERM_ID),
                eq(USER_ID),
                eq("TIMETABLE"),
                eq("CSV"),
                eq("timetable.csv"),
                eq("CSV"),
                any(),
                any(),
                any(),
                eq(1),
                eq(false));
    }

    @Test
    void preview_rejectsDisguisedSpreadsheet() {
        stubScope();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "unsafe.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "not a zip workbook".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.preview(
                USER_ID, TERM_ID, "TIMETABLE", null, file))
                .isInstanceOf(AuraStateException.class)
                .hasMessage("The file content does not match its filename extension.");
    }

    @Test
    void previewTextlessPdf_returnsOcrRequiredWithoutClaimingOcrSupport()
            throws Exception {
        stubScope();
        byte[] pdf;
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            pdf = output.toByteArray();
        }
        MockMultipartFile file = new MockMultipartFile(
                "file", "scan.pdf", "application/pdf", pdf);

        var response = service.preview(
                USER_ID, TERM_ID, "TIMETABLE", null, file);

        assertThat(response.ocrRequired()).isTrue();
        assertThat(response.warnings()).anyMatch(message ->
                message.startsWith("OCR_REQUIRED"));
    }

    @Test
    void validate_rejectsInvalidRowsAndPersistsActionableIssues() {
        UUID jobId = UUID.randomUUID();
        when(authorizationService.requireAdminUniversity(USER_ID))
                .thenReturn(UNIVERSITY_ID);
        when(importRepository.findJob(jobId)).thenReturn(Optional.of(
                new ImportJob(
                        jobId, UNIVERSITY_ID, TERM_ID, "ROOMS", "PREVIEWED",
                        List.of("Code", "Name", "Capacity", "Room Type"),
                        Map.of(), Map.of(), 1, 0, null, null, USER_ID)));
        when(importRepository.listSourceRows(jobId)).thenReturn(List.of(
                new SourceRow(1, Map.of(
                        "Code", "R1", "Name", "Main room",
                        "Capacity", "not-a-number", "Room Type", "CLASSROOM"))));
        Map<String, String> mapping = Map.of(
                "CODE", "Code", "NAME", "Name", "CAPACITY", "Capacity",
                "ROOM_TYPE", "Room Type");

        var response = service.validate(
                USER_ID, jobId,
                new com.campusone.aura.dto.AuraImportDtos.ValidateImportRequest(
                        mapping, null));

        assertThat(response.status()).isEqualTo("PREVIEWED");
        assertThat(response.rejectedRows()).isEqualTo(1);
        assertThat(response.issues()).singleElement()
                .satisfies(issue -> {
                    assertThat(issue.field()).isEqualTo("CAPACITY");
                    assertThat(issue.code()).isEqualTo("INVALID_NUMBER");
                });
        verify(importRepository).markValidated(jobId, mapping, 0, 1);
    }

    @Test
    void apply_rejectsJobThatHasNotPassedValidation() {
        UUID jobId = UUID.randomUUID();
        when(authorizationService.requireAdminUniversity(USER_ID))
                .thenReturn(UNIVERSITY_ID);
        when(importRepository.findJob(jobId)).thenReturn(Optional.of(
                new ImportJob(
                        jobId, UNIVERSITY_ID, TERM_ID, "ROOMS", "PREVIEWED",
                        List.of("Code"), Map.of(), Map.of(), 1, 0,
                        null, null, USER_ID)));

        assertThatThrownBy(() -> service.apply(USER_ID, jobId))
                .isInstanceOf(AuraStateException.class)
                .hasMessage("Validate every import row successfully before applying it.");
    }

    @Test
    void applyTimetable_createsDraftAndRunsIndependentClashDetection() {
        UUID jobId = UUID.randomUUID();
        UUID requirementId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID timeslotId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        Map<String, String> mapping = Map.of(
                "COURSE", "Course", "SECTION", "Section",
                "INSTRUCTOR", "Instructor", "ROOM", "Room",
                "DAY", "Day", "START", "Start");
        when(authorizationService.requireAdminUniversity(USER_ID))
                .thenReturn(UNIVERSITY_ID);
        when(importRepository.findJob(jobId)).thenReturn(Optional.of(
                new ImportJob(
                        jobId, UNIVERSITY_ID, TERM_ID, "TIMETABLE", "VALIDATED",
                        List.copyOf(mapping.values()), Map.of(), mapping, 1, 0,
                        null, null, USER_ID)));
        when(auraRepository.resourceBelongsToUniversity(
                ScopedResource.TERM, TERM_ID, UNIVERSITY_ID)).thenReturn(true);
        when(importRepository.listSourceRows(jobId)).thenReturn(List.of(
                new SourceRow(1, Map.of(
                        "Course", "CS101", "Section", "A",
                        "Instructor", "I-1", "Room", "R1",
                        "Day", "Monday", "Start", "09:00"))));
        when(auraRepository.nextVersionNumber(TERM_ID)).thenReturn(2);
        when(importRepository.insertImportedVersion(
                TERM_ID, 2, USER_ID, "Imported timetable"))
                .thenReturn(versionId);
        when(importRepository.findRequirement(TERM_ID, "CS101", "LECTURE"))
                .thenReturn(Optional.of(requirementId));
        when(importRepository.findRoomForTerm(TERM_ID, "R1"))
                .thenReturn(Optional.of(roomId));
        when(importRepository.findTimeslotForTerm(
                TERM_ID, 1, java.time.LocalTime.of(9, 0)))
                .thenReturn(Optional.of(timeslotId));
        when(auraRepository.listSessions(versionId)).thenReturn(List.of());
        when(clashDetector.detect(List.of())).thenReturn(List.of());
        ImportApplyResponse applied = new ImportApplyResponse(
                jobId, "TIMETABLE", "APPLIED", 1, 0, versionId,
                "Import applied successfully.");
        when(importRepository.applyResponse(jobId)).thenReturn(applied);

        assertThat(service.apply(USER_ID, jobId)).isEqualTo(applied);

        verify(importRepository).markApplying(jobId);
        verify(importRepository).insertImportedSession(
                versionId, requirementId, roomId, timeslotId, 1);
        verify(auraRepository).replaceClashes(versionId, List.of());
        verify(importRepository).markApplied(jobId, 1, versionId);
    }

    private void stubScope() {
        when(authorizationService.requireAdminUniversity(USER_ID))
                .thenReturn(UNIVERSITY_ID);
        when(auraRepository.resourceBelongsToUniversity(
                ScopedResource.TERM, TERM_ID, UNIVERSITY_ID)).thenReturn(true);
    }
}
