package com.campusone.aura.service;

import com.campusone.aura.dto.AuraDtos.SessionResponse;
import com.campusone.aura.repository.AuraJdbcRepository;
import com.campusone.aura.repository.AuraJdbcRepository.ScopedResource;
import com.campusone.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
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
        "Starts", "Ends", "Source"
    };

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
        if (sessions.size() > MAX_EXPORT_ROWS) {
            throw new IllegalStateException(
                    "This timetable is too large to export in a single file.");
        }
        String format = requestedFormat == null
                ? "CSV"
                : requestedFormat.trim().toUpperCase(Locale.ROOT);
        try {
            return switch (format) {
                case "CSV" -> payload("csv", "text/csv", csv(sessions));
                case "JSON" -> payload(
                        "json", "application/json",
                        objectMapper.writeValueAsBytes(sessions));
                case "XLSX" -> payload(
                        "xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        xlsx(sessions));
                case "HTML" -> payload(
                        "html", "text/html", html(sessions));
                case "ICS" -> payload(
                        "ics", "text/calendar", ics(sessions));
                case "PDF" -> payload(
                        "pdf", "application/pdf", pdf(sessions));
                default -> throw new IllegalArgumentException(
                        "Choose CSV, XLSX, JSON, HTML, ICS, or PDF.");
            };
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

    private byte[] csv(List<SessionResponse> sessions) {
        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", HEADERS)).append("\r\n");
        sessions.forEach(session -> csv.append(String.join(",",
                csvCell(session.courseCode()),
                csvCell(session.courseTitle()),
                csvCell(session.sectionName()),
                csvCell(session.instructorName()),
                csvCell(session.roomName()),
                Integer.toString(session.dayOfWeek()),
                session.startsAt().toString(),
                session.endsAt().toString(),
                csvCell(session.source()))).append("\r\n"));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] xlsx(List<SessionResponse> sessions) throws IOException {
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
                String[] values = rowValues(session);
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

    private byte[] html(List<SessionResponse> sessions) {
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
            for (String value : rowValues(session)) {
                html.append("<td>").append(escapeHtml(value)).append("</td>");
            }
            html.append("</tr>");
        });
        html.append("</tbody></table></body></html>");
        return html.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] ics(List<SessionResponse> sessions) {
        StringBuilder ics = new StringBuilder(
                "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//CampusOne//AURA//EN\r\n");
        sessions.forEach(session -> ics.append("BEGIN:VEVENT\r\nUID:")
                .append(session.id()).append("@campusone.dev\r\nSUMMARY:")
                .append(escapeIcs(session.courseCode() + " " + session.courseTitle()))
                .append("\r\nDESCRIPTION:")
                .append(escapeIcs(session.sectionName() + " · " + session.instructorName()))
                .append("\r\nLOCATION:").append(escapeIcs(session.roomName()))
                .append("\r\nX-AURA-DAY-OF-WEEK:").append(session.dayOfWeek())
                .append("\r\nX-AURA-START-TIME:").append(session.startsAt())
                .append("\r\nX-AURA-END-TIME:").append(session.endsAt())
                .append("\r\nEND:VEVENT\r\n"));
        ics.append("END:VCALENDAR\r\n");
        return ics.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] pdf(List<SessionResponse> sessions) throws IOException {
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
                                + "-" + session.endsAt().format(DateTimeFormatter.ofPattern("HH:mm"));
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

    private String[] rowValues(SessionResponse session) {
        return new String[] {
            session.courseCode(), session.courseTitle(), session.sectionName(),
            session.instructorName(), session.roomName(),
            Integer.toString(session.dayOfWeek()), session.startsAt().toString(),
            session.endsAt().toString(), session.source()
        };
    }

    private String csvCell(String value) {
        String safe = value == null ? "" : value;
        return '"' + safe.replace("\"", "\"\"") + '"';
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
}
