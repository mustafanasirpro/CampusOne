package com.campusone.aura.service;

import com.campusone.aura.dto.AuraDtos.SessionResponse;
import com.campusone.aura.dto.AuraDtos.TermResponse;
import com.campusone.aura.dto.AuraDtos.TimetableVersionResponse;
import com.campusone.aura.repository.AuraJdbcRepository;
import com.campusone.aura.repository.AuraJdbcRepository.ScopedResource;
import com.campusone.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        prefix = "campusone.aura",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuraExportService {

    private static final int MAX_EXPORT_ROWS = 10_000;
    private static final String[] HEADERS = {
        "Course", "Title", "Section", "Instructor", "Room", "Day",
        "Starts", "Ends", "Week pattern", "Custom weeks",
        "Cancelled dates", "Source"
    };
    private static final DateTimeFormatter ICS_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private final AuraAuthorizationService authorizationService;
    private final AuraJdbcRepository repository;
    private final ObjectMapper objectMapper;

    public AuraExportService(
            AuraAuthorizationService authorizationService,
            AuraJdbcRepository repository,
            ObjectMapper objectMapper) {
        this.authorizationService = authorizationService;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public ExportPayload export(UUID userId, UUID versionId, String requestedFormat) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        if (!repository.resourceBelongsToUniversity(
                ScopedResource.VERSION, versionId, universityId)) {
            throw new ResourceNotFoundException("AURA timetable version");
        }
        List<SessionResponse> sessions = repository.listSessions(versionId);
        TimetableVersionResponse version = repository.findVersion(versionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "AURA timetable version"));
        TermResponse term = repository.findTerm(version.termId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "AURA academic term"));
        Map<UUID, List<LocalDate>> cancellations =
                repository.calendarCancellationDates(versionId);
        if (sessions.size() > MAX_EXPORT_ROWS) {
            throw new IllegalStateException(
                    "This timetable is too large to export in a single file.");
        }
        String format = requestedFormat == null
                ? "CSV"
                : requestedFormat.trim().toUpperCase(Locale.ROOT);
        try {
            ExportPayload payload = switch (format) {
                case "CSV" -> payload(
                        "csv", "text/csv", csv(sessions, cancellations));
                case "JSON" -> payload(
                        "json", "application/json",
                        objectMapper.writeValueAsBytes(exportRows(
                                sessions, cancellations)));
                case "XLSX" -> payload(
                        "xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        xlsx(sessions, cancellations));
                case "HTML" -> payload(
                        "html", "text/html", html(sessions, cancellations));
                case "ICS" -> payload(
                        "ics", "text/calendar",
                        ics(sessions, term, cancellations));
                case "PDF" -> payload(
                        "pdf", "application/pdf", pdf(sessions, cancellations));
                default -> throw new IllegalArgumentException(
                        "Choose CSV, XLSX, JSON, HTML, ICS, or PDF.");
            };
            repository.insertVersionAudit(
                    versionId,
                    userId,
                    "TIMETABLE_EXPORTED",
                    "Exported a timetable as " + format + ".");
            return payload;
        } catch (IOException exception) {
            throw new IllegalStateException("The timetable export could not be created.", exception);
        }
    }

    private ExportPayload payload(String extension, String contentType, byte[] bytes) {
        return new ExportPayload(
                "aura-timetable." + extension,
                contentType,
                bytes);
    }

    private byte[] csv(
            List<SessionResponse> sessions,
            Map<UUID, List<LocalDate>> cancellations) {
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", HEADERS)).append("\r\n");
        sessions.forEach(session -> csv.append(String.join(
                ",", escapedRowValues(session, cancellations))).append("\r\n"));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] xlsx(
            List<SessionResponse> sessions,
            Map<UUID, List<LocalDate>> cancellations) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Timetable");
            Row header = sheet.createRow(0);
            for (int index = 0; index < HEADERS.length; index++) {
                header.createCell(index).setCellValue(HEADERS[index]);
            }
            int rowNumber = 1;
            for (SessionResponse session : sessions) {
                Row row = sheet.createRow(rowNumber++);
                String[] values = rowValues(session, cancellations);
                for (int index = 0; index < values.length; index++) {
                    row.createCell(index).setCellValue(values[index]);
                }
            }
            for (int index = 0; index < HEADERS.length; index++) {
                sheet.autoSizeColumn(index);
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private byte[] html(
            List<SessionResponse> sessions,
            Map<UUID, List<LocalDate>> cancellations) {
        StringBuilder html = new StringBuilder("""
                <!doctype html><html><head><meta charset="utf-8">
                <title>AURA timetable</title><style>
                body{font-family:system-ui,sans-serif;margin:2rem;color:#172033}
                table{border-collapse:collapse;width:100%}th,td{border:1px solid #d9deea;padding:.55rem;text-align:left}
                th{background:#f3f5f9}@media print{body{margin:0}}
                </style></head><body><h1>AURA timetable</h1><table><thead><tr>
                """);
        for (String header : HEADERS) html.append("<th>").append(header).append("</th>");
        html.append("</tr></thead><tbody>");
        sessions.forEach(session -> {
            html.append("<tr>");
            for (String value : rowValues(session, cancellations)) {
                html.append("<td>").append(escapeHtml(value)).append("</td>");
            }
            html.append("</tr>");
        });
        html.append("</tbody></table></body></html>");
        return html.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] ics(
            List<SessionResponse> sessions,
            TermResponse term,
            Map<UUID, List<LocalDate>> cancellations) {
        String timezone = safeTimezone(term.timezone());
        StringBuilder ics = new StringBuilder(
                "BEGIN:VCALENDAR\r\nVERSION:2.0\r\n"
                        + "PRODID:-//CampusOne//AURA//EN\r\n"
                        + "CALSCALE:GREGORIAN\r\nMETHOD:PUBLISH\r\n");
        sessions.forEach(session -> {
            List<LocalDate> occurrences = occurrenceDates(
                    term,
                    session,
                    cancellations.getOrDefault(session.id(), List.of()));
            if (occurrences.isEmpty()) {
                return;
            }
            LocalDate first = occurrences.getFirst();
            ics.append("BEGIN:VEVENT\r\nUID:")
                .append(session.id()).append("@campusone.dev\r\nDTSTART;TZID=")
                .append(timezone).append(':')
                .append(icsDateTime(first, session.startsAt()))
                .append("\r\nDTEND;TZID=").append(timezone).append(':')
                .append(icsDateTime(first, session.endsAt()))
                .append("\r\nSUMMARY:")
                .append(escapeIcs(session.courseCode() + " " + session.courseTitle()))
                .append("\r\nDESCRIPTION:")
                .append(escapeIcs(session.sectionName() + " · " + session.instructorName()))
                .append("\r\nLOCATION:").append(escapeIcs(session.roomName()))
                .append("\r\nX-AURA-DAY-OF-WEEK:").append(session.dayOfWeek())
                .append("\r\nX-AURA-START-TIME:").append(session.startsAt())
                .append("\r\nX-AURA-END-TIME:").append(session.endsAt())
                .append("\r\nX-AURA-WEEK-PATTERN:")
                .append(session.weekPattern());
            if (occurrences.size() > 1) {
                ics.append("\r\nRDATE;TZID=").append(timezone).append(':')
                        .append(occurrences.subList(1, occurrences.size()).stream()
                                .map(date -> icsDateTime(
                                        date, session.startsAt()))
                                .collect(java.util.stream.Collectors.joining(",")));
            }
            ics.append("\r\nEND:VEVENT\r\n");
        });
        ics.append("END:VCALENDAR\r\n");
        return ics.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] pdf(
            List<SessionResponse> sessions,
            Map<UUID, List<LocalDate>> cancellations) throws IOException {
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDPage page = null;
            PDPageContentStream content = null;
            float y = 0;
            try {
                for (int index = -1; index < sessions.size(); index++) {
                    if (page == null || y < 55) {
                        if (content != null) content.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        content = new PDPageContentStream(document, page);
                        y = 800;
                        writePdfLine(content, bold, 15, 45, y, "AURA timetable");
                        y -= 24;
                    }
                    if (index >= 0) {
                        SessionResponse session = sessions.get(index);
                        String line = session.courseCode() + " | "
                                + session.sectionName() + " | " + session.roomName()
                                + " | Day " + session.dayOfWeek() + " "
                                + session.startsAt().format(DateTimeFormatter.ofPattern("HH:mm"))
                                + "-" + session.endsAt().format(DateTimeFormatter.ofPattern("HH:mm"))
                                + " | " + session.weekPattern()
                                + " | " + cancellations.getOrDefault(
                                        session.id(), List.of()).size()
                                + " cancelled";
                        writePdfLine(content, font, 9, 45, y, truncate(line, 105));
                        y -= 15;
                    }
                }
            } finally {
                if (content != null) content.close();
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private void writePdfLine(
            PDPageContentStream content,
            PDType1Font font,
            float size,
            float x,
            float y,
            String text) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(text.replaceAll("[^\\x20-\\x7E]", " "));
        content.endText();
    }

    private String[] rowValues(
            SessionResponse session,
            Map<UUID, List<LocalDate>> cancellations) {
        return new String[] {
            session.courseCode(), session.courseTitle(), session.sectionName(),
            session.instructorName(), session.roomName(),
            Integer.toString(session.dayOfWeek()), session.startsAt().toString(),
            session.endsAt().toString(), session.weekPattern(),
            session.customWeeks().stream().map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(",")),
            cancellations.getOrDefault(session.id(), List.of()).stream()
                    .map(LocalDate::toString)
                    .collect(java.util.stream.Collectors.joining(",")),
            session.source()
        };
    }

    private String[] escapedRowValues(
            SessionResponse session,
            Map<UUID, List<LocalDate>> cancellations) {
        return java.util.Arrays.stream(rowValues(session, cancellations))
                .map(this::csvCell)
                .toArray(String[]::new);
    }

    private List<ExportRow> exportRows(
            List<SessionResponse> sessions,
            Map<UUID, List<LocalDate>> cancellations) {
        return sessions.stream()
                .map(session -> new ExportRow(
                        session,
                        cancellations.getOrDefault(session.id(), List.of())))
                .toList();
    }

    private List<LocalDate> occurrenceDates(
            TermResponse term,
            SessionResponse session,
            List<LocalDate> cancellations) {
        Set<LocalDate> cancelled = Set.copyOf(cancellations);
        LocalDate first = term.startsOn().with(TemporalAdjusters.nextOrSame(
                DayOfWeek.of(session.dayOfWeek())));
        java.util.ArrayList<LocalDate> dates = new java.util.ArrayList<>();
        for (LocalDate date = first;
                !date.isAfter(term.endsOn());
                date = date.plusWeeks(1)) {
            int week = (int) ChronoUnit.WEEKS.between(term.startsOn(), date) + 1;
            if (occursInWeek(session, week) && !cancelled.contains(date)) {
                dates.add(date);
            }
        }
        return List.copyOf(dates);
    }

    private boolean occursInWeek(SessionResponse session, int week) {
        return switch (session.weekPattern()) {
            case "ODD_WEEK" -> week % 2 == 1;
            case "EVEN_WEEK" -> week % 2 == 0;
            case "CUSTOM_WEEK_SET" -> session.customWeeks().contains(week);
            default -> true;
        };
    }

    private String icsDateTime(LocalDate date, LocalTime time) {
        return date.atTime(time).format(ICS_DATE_TIME);
    }

    private String safeTimezone(String timezone) {
        if (timezone == null
                || !timezone.matches("[A-Za-z0-9_+.-]+(?:/[A-Za-z0-9_+.-]+)*")) {
            return "UTC";
        }
        return timezone;
    }

    private String csvCell(String value) {
        String safe = spreadsheetSafe(value);
        return '"' + safe.replace("\"", "\"\"") + '"';
    }

    private String spreadsheetSafe(String value) {
        String safe = value == null ? "" : value;
        if (!safe.isEmpty() && "=+-@\t\r".indexOf(safe.charAt(0)) >= 0) {
            return "'" + safe;
        }
        return safe;
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String escapeIcs(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace(";", "\\;")
                .replace(",", "\\,").replace("\n", "\\n");
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength
                ? value
                : value.substring(0, maxLength - 1) + "…";
    }

    public record ExportPayload(String filename, String contentType, byte[] bytes) {
    }

    private record ExportRow(
            SessionResponse session,
            List<LocalDate> cancelledDates) {
    }
}
