package com.campusone.aura.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.campusone.aura.dto.AuraDtos.SessionResponse;
import com.campusone.aura.repository.AuraJdbcRepository;
import com.campusone.aura.repository.AuraJdbcRepository.ScopedResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
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

    @Mock
    private AuraAuthorizationService authorizationService;

    @Mock
    private AuraJdbcRepository repository;

    private AuraExportService service;

    @BeforeEach
    void setUp() {
        service = new AuraExportService(
                authorizationService, repository, new ObjectMapper());
        when(authorizationService.requireAdminUniversity(USER_ID))
                .thenReturn(UNIVERSITY_ID);
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

    private SessionResponse session() {
        return new SessionResponse(
                UUID.randomUUID(), VERSION_ID, UUID.randomUUID(), UUID.randomUUID(),
                "CS101", "Programming, Fundamentals", UUID.randomUUID(),
                "BSCS-1A", UUID.randomUUID(), "Dr Ahmed", UUID.randomUUID(),
                "Room 1", "CLASSROOM", UUID.randomUUID(), 1,
                LocalTime.of(9, 0), LocalTime.of(10, 0), false, "SOLVER");
    }
}
