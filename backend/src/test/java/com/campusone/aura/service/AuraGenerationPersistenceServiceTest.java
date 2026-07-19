package com.campusone.aura.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.aura.exception.AuraStateException;
import com.campusone.aura.repository.AuraJdbcRepository;
import com.campusone.aura.repository.AuraJdbcRepository.RunPersistenceState;
import com.campusone.aura.repository.AuraJdbcRepository.SolverAssignment;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuraGenerationPersistenceServiceTest {

    private static final UUID RUN_ID = id(1);
    private static final UUID TERM_ID = id(2);
    private static final UUID USER_ID = id(3);
    private static final UUID VERSION_ID = id(4);

    @Mock
    private AuraJdbcRepository repository;

    @Mock
    private AuraClashDetector clashDetector;

    private AuraGenerationPersistenceService service;

    @BeforeEach
    void setUp() {
        service = new AuraGenerationPersistenceService(repository, clashDetector);
    }

    @Test
    void persistCompletedRun_rejectsChangedSchedulingDataBeforeWriting() {
        when(repository.lockRunForPersistence(RUN_ID, TERM_ID))
                .thenReturn(new RunPersistenceState("RUNNING", false));

        assertThatThrownBy(() -> service.persistCompletedRun(
                RUN_ID, TERM_ID, USER_ID, "0hard/0medium/0soft", null,
                List.of()))
                .isInstanceOf(AuraStateException.class)
                .hasMessageContaining("Scheduling data changed");

        verify(repository, never()).insertVersion(
                any(), any(), any(), any(Integer.class), any(), any(), any());
    }

    @Test
    void persistCompletedRun_rejectsCancelledRunBeforeWriting() {
        when(repository.lockRunForPersistence(RUN_ID, TERM_ID))
                .thenReturn(new RunPersistenceState("CANCELLED", true));

        assertThatThrownBy(() -> service.persistCompletedRun(
                RUN_ID, TERM_ID, USER_ID, "0hard/0medium/0soft", null,
                List.of()))
                .isInstanceOf(AuraStateException.class)
                .hasMessage("This generation run is no longer active.");

        verify(repository, never()).insertVersion(
                any(), any(), any(), any(Integer.class), any(), any(), any());
    }

    @Test
    void persistCompletedRun_writesScheduleClashesAndCompletionTogether() {
        SolverAssignment assignment = new SolverAssignment(
                id(5), id(6), id(7), id(8), id(9), id(10));
        when(repository.lockRunForPersistence(RUN_ID, TERM_ID))
                .thenReturn(new RunPersistenceState("RUNNING", true));
        when(repository.nextVersionNumber(TERM_ID)).thenReturn(2);
        when(repository.insertVersion(
                any(), eq(TERM_ID), eq(RUN_ID), eq(2),
                eq("0hard/0medium/0soft"), eq("verified"), eq(USER_ID)))
                .thenReturn(VERSION_ID);
        when(repository.listSessions(VERSION_ID)).thenReturn(List.of());
        when(clashDetector.detect(List.of())).thenReturn(List.of());
        when(repository.markRunCompleted(
                RUN_ID,
                "0hard/0medium/0soft",
                "Timetable generated with 1 scheduled sessions."))
                .thenReturn(true);

        UUID persisted = service.persistCompletedRun(
                RUN_ID,
                TERM_ID,
                USER_ID,
                "0hard/0medium/0soft",
                "verified",
                List.of(assignment));

        assertThat(persisted).isEqualTo(VERSION_ID);
        verify(repository).insertSession(any(), eq(VERSION_ID),
                eq(assignment), eq("SOLVER"));
        verify(repository).replaceClashes(VERSION_ID, List.of());
        verify(repository).markRunCompleted(
                RUN_ID,
                "0hard/0medium/0soft",
                "Timetable generated with 1 scheduled sessions.");
    }

    private static UUID id(int suffix) {
        return UUID.fromString(
                "a0000000-0000-4000-8000-" + String.format("%012d", suffix));
    }
}
