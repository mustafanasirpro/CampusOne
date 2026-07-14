package com.campusone.aura.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusone.aura.repository.AuraJdbcRepository.SolverRequirement;
import com.campusone.aura.repository.AuraJdbcRepository.SolverInstructorAvailability;
import com.campusone.aura.repository.AuraJdbcRepository.SolverRoomAvailability;
import com.campusone.aura.repository.AuraJdbcRepository.SolverRoom;
import com.campusone.aura.repository.AuraJdbcRepository.SolverTimeslot;
import com.campusone.aura.service.AuraSolverService.SolverResult;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuraSolverServiceTest {

    @Test
    void solve_tinyFeasibleDataset_assignsEveryRequiredSession() {
        UUID instructor = UUID.fromString(
                "90000000-0000-4000-8000-000000000001");
        UUID section = UUID.fromString(
                "90000000-0000-4000-8000-000000000002");
        SolverResult result = new AuraSolverService().solve(
                List.of(new SolverRequirement(
                        UUID.fromString("90000000-0000-4000-8000-000000000003"),
                        UUID.fromString("90000000-0000-4000-8000-000000000004"),
                        section,
                        instructor,
                        2,
                        "CLASSROOM",
                        30)),
                List.of(
                        new SolverRoom(
                                UUID.fromString("90000000-0000-4000-8000-000000000005"),
                                40,
                                "CLASSROOM"),
                        new SolverRoom(
                                UUID.fromString("90000000-0000-4000-8000-000000000006"),
                                40,
                                "CLASSROOM")),
                List.of(
                        new SolverTimeslot(
                                UUID.fromString("90000000-0000-4000-8000-000000000007"),
                                1,
                                LocalTime.of(9, 0),
                                LocalTime.of(10, 0)),
                        new SolverTimeslot(
                                UUID.fromString("90000000-0000-4000-8000-000000000008"),
                                2,
                                LocalTime.of(9, 0),
                                LocalTime.of(10, 0))),
                List.of(),
                List.of(),
                1);

        assertThat(result.assignments()).hasSize(2);
        assertThat(result.assignments())
                .allSatisfy(assignment -> {
                    assertThat(assignment.roomId()).isNotNull();
                    assertThat(assignment.timeslotId()).isNotNull();
                });
        assertThat(result.score()).contains("hard");
    }

    @Test
    void solve_respectsInstructorUnavailableTimeslotAsHardConstraint() {
        UUID instructor = UUID.fromString(
                "90000000-0000-4000-8000-000000000011");
        UUID section = UUID.fromString(
                "90000000-0000-4000-8000-000000000012");
        UUID unavailableTimeslot = UUID.fromString(
                "90000000-0000-4000-8000-000000000017");
        UUID availableTimeslot = UUID.fromString(
                "90000000-0000-4000-8000-000000000018");

        SolverResult result = new AuraSolverService().solve(
                List.of(new SolverRequirement(
                        UUID.fromString("90000000-0000-4000-8000-000000000013"),
                        UUID.fromString("90000000-0000-4000-8000-000000000014"),
                        section,
                        instructor,
                        1,
                        "CLASSROOM",
                        30)),
                List.of(new SolverRoom(
                        UUID.fromString("90000000-0000-4000-8000-000000000015"),
                        40,
                        "CLASSROOM")),
                List.of(
                        new SolverTimeslot(
                                unavailableTimeslot,
                                1,
                                LocalTime.of(9, 0),
                                LocalTime.of(10, 0)),
                        new SolverTimeslot(
                                availableTimeslot,
                                2,
                                LocalTime.of(9, 0),
                                LocalTime.of(10, 0))),
                List.of(new SolverInstructorAvailability(
                        instructor,
                        unavailableTimeslot,
                        "UNAVAILABLE")),
                List.of(),
                1);

        assertThat(result.assignments()).hasSize(1);
        assertThat(result.assignments().getFirst().timeslotId())
                .isEqualTo(availableTimeslot);
        assertThat(result.score()).startsWith("0hard");
    }
}
