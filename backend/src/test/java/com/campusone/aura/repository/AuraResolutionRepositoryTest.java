package com.campusone.aura.repository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.aura.repository.AuraResolutionRepository.OfferingCandidate;
import com.campusone.aura.repository.AuraResolutionRepository.RegistrationScope;
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
class AuraResolutionRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate jdbc;

    private AuraResolutionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new AuraResolutionRepository(jdbc);
    }

    @Test
    @SuppressWarnings("unchecked")
    void offeringCandidatesIncludeConfiguredSectionEquivalentAndMakeupStrategies() {
        when(jdbc.query(
                any(String.class),
                any(SqlParameterSource.class),
                any(RowMapper.class))).thenReturn(List.of());
        RegistrationScope registration = new RegistrationScope(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "MAKEUP", UUID.randomUUID(), null, null, null, 0);

        repository.parallelOfferingCandidates(registration);

        verify(jdbc).query(
                argThat(sql -> sql.contains("'SECTION_TRANSFER'")
                        && sql.contains("'EQUIVALENT_OFFERING'")
                        && sql.contains("'MAKEUP_SESSION'")
                        && sql.contains("registration_compatible")
                        && sql.contains("section_row.active = TRUE")),
                argThat((SqlParameterSource parameters) -> "MAKEUP".equals(
                        parameters.getValue("registrationType"))
                        && registration.equivalentOfferingId().equals(
                                parameters.getValue("equivalentOfferingId"))),
                any(RowMapper.class));
    }

    @Test
    void persistedSuggestionUsesCandidateStrategyAndMarksGroupMismatchUnsafe() {
        UUID caseId = UUID.randomUUID();
        OfferingCandidate candidate = new OfferingCandidate(
                UUID.randomUUID(), "EQUIVALENT_OFFERING", "CS-EQ",
                "Equivalent course", UUID.randomUUID(), "Section E",
                5, true, false);

        repository.replaceSuggestions(caseId, List.of(candidate), List.of());

        verify(jdbc).update(
                argThat(sql -> sql.contains(":suggestionType")
                        && !sql.contains("'PARALLEL_OFFERING_TRANSFER',")),
                argThat((SqlParameterSource parameters) ->
                        "EQUIVALENT_OFFERING".equals(
                                parameters.getValue("suggestionType"))
                        && Boolean.FALSE.equals(parameters.getValue("safe"))
                        && Integer.valueOf(1).equals(
                                parameters.getValue("hardAdded"))));
    }

    @Test
    void persistedOfferingSuggestionKeepsAtomicTeachingGroupAssignment() {
        UUID caseId = UUID.randomUUID();
        UUID lectureGroupId = UUID.randomUUID();
        UUID labGroupId = UUID.randomUUID();
        UUID tutorialGroupId = UUID.randomUUID();
        OfferingCandidate candidate = new OfferingCandidate(
                UUID.randomUUID(), "SECTION_TRANSFER", "CS101-B",
                "Programming", UUID.randomUUID(), "Section B", 5,
                true, true, lectureGroupId, labGroupId, tutorialGroupId);

        repository.replaceSuggestions(caseId, List.of(candidate), List.of());

        verify(jdbc).update(
                argThat(sql -> sql.contains("target_lecture_group_id")
                        && sql.contains("target_lab_group_id")
                        && sql.contains("target_tutorial_group_id")),
                argThat((SqlParameterSource parameters) ->
                        lectureGroupId.equals(
                                parameters.getValue("lectureGroupId"))
                        && labGroupId.equals(
                                parameters.getValue("labGroupId"))
                        && tutorialGroupId.equals(
                                parameters.getValue("tutorialGroupId"))
                        && caseId.equals(parameters.getValue("caseId"))));
    }
}
