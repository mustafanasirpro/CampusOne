package com.campusone.aura.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.campusone.aura.dto.AuraDtos.ReadinessResponse;
import com.campusone.aura.repository.AuraJdbcRepository;
import com.campusone.aura.repository.AuraJdbcRepository.TermCounts;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuraReadinessValidatorTest {

    private static final UUID TERM_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");

    @Mock
    private AuraJdbcRepository repository;

    @Test
    void validate_missingSetup_returnsActionableIssues() {
        when(repository.countsForTerm(TERM_ID))
                .thenReturn(new TermCounts(0, 0, 0, 0, 0));

        ReadinessResponse response =
                new AuraReadinessValidator(repository).validate(TERM_ID);

        assertThat(response.ready()).isFalse();
        assertThat(response.issues())
                .extracting(issue -> issue.code())
                .contains(
                        "AURA_NO_ROOMS",
                        "AURA_NO_TIMESLOTS",
                        "AURA_NO_INSTRUCTORS",
                        "AURA_NO_OFFERINGS",
                        "AURA_NO_MEETING_REQUIREMENTS");
    }

    @Test
    void validate_completeSmallDataset_isReady() {
        when(repository.countsForTerm(TERM_ID))
                .thenReturn(new TermCounts(3, 6, 2, 2, 4));

        ReadinessResponse response =
                new AuraReadinessValidator(repository).validate(TERM_ID);

        assertThat(response.ready()).isTrue();
        assertThat(response.issues()).isEmpty();
    }
}
