package com.campusone.aura.repository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
class AuraJdbcRepositoryScopeTest {

    @Mock
    private NamedParameterJdbcTemplate jdbc;

    private AuraJdbcRepository repository;

    @BeforeEach
    void setUp() {
        repository = new AuraJdbcRepository(jdbc);
    }

    @Test
    @SuppressWarnings("unchecked")
    void solverTimeslotsExcludeOtherTerms() {
        when(jdbc.query(
                any(String.class),
                any(SqlParameterSource.class),
                any(RowMapper.class))).thenReturn(List.of());

        repository.solverTimeslots(UUID.randomUUID());

        verify(jdbc).query(
                argThat(sql -> sql.contains(
                        "(ts.term_id IS NULL OR ts.term_id = :termId)")),
                any(SqlParameterSource.class),
                any(RowMapper.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void solverRequirementsRequireActiveSameUniversityFacts() {
        when(jdbc.query(
                any(String.class),
                any(SqlParameterSource.class),
                any(RowMapper.class))).thenReturn(List.of());

        repository.solverRequirements(UUID.randomUUID());

        verify(jdbc).query(
                argThat(sql -> sql.contains(
                        "course_department.university_id = term_row.university_id")
                        && sql.contains(
                                "i.university_id = term_row.university_id")
                        && sql.contains(
                                "program.university_id = term_row.university_id")
                        && sql.contains(
                                "linked_program.university_id =")
                        && sql.contains("registration.university_id =")),
                any(SqlParameterSource.class),
                any(RowMapper.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void solverRoomsUseNormalizedBuildingForTravelFacts() {
        when(jdbc.query(
                any(String.class),
                any(SqlParameterSource.class),
                any(RowMapper.class))).thenReturn(List.of());

        repository.solverRooms(UUID.randomUUID());

        verify(jdbc).query(
                argThat(sql -> sql.contains(
                        "COALESCE(building.name, r.building, '') AS building")
                        && sql.contains(
                                "LEFT JOIN aura_buildings building")),
                any(SqlParameterSource.class),
                any(RowMapper.class));
    }
}
