package com.campusone.aura.repository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

@ExtendWith(MockitoExtension.class)
class AuraScenarioRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate jdbc;

    private AuraScenarioRepository repository;

    @BeforeEach
    void setUp() {
        repository = new AuraScenarioRepository(jdbc, new ObjectMapper());
    }

    @Test
    void affectedSessionCountSeparatesDynamicRoomPredicate() {
        when(jdbc.queryForObject(
                any(String.class), any(SqlParameterSource.class), eq(Integer.class)))
                .thenReturn(1);

        repository.countAffectedSessions(UUID.randomUUID(), "ROOM_CLOSURE", UUID.randomUUID());

        verify(jdbc).queryForObject(
                argThat(sql -> sql.contains("AND room_id = :resourceId")),
                any(SqlParameterSource.class), eq(Integer.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void affectedSessionListSeparatesDynamicTimeslotPredicate() {
        when(jdbc.query(
                any(String.class), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        repository.listAffectedSessionIds(
                UUID.randomUUID(), "TIMESLOT_CANCELLATION", UUID.randomUUID());

        verify(jdbc).query(
                argThat(sql -> sql.contains("AND timeslot_id = :resourceId")),
                any(SqlParameterSource.class), any(RowMapper.class));
    }

    @Test
    void emergencyDraftPinningSeparatesDynamicInstructorPredicate() {
        repository.completeEmergencyDraft(
                UUID.randomUUID(), UUID.randomUUID(),
                "INSTRUCTOR_ABSENCE", UUID.randomUUID());

        verify(jdbc).update(
                argThat(sql -> sql.contains("AND instructor_id <> :affectedResourceId")),
                any(SqlParameterSource.class));
    }
}
