package com.campusone.aura.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusone.aura.repository.AuraJdbcRepository.SolverRequirement;
import com.campusone.aura.repository.AuraJdbcRepository.SolverInstructorAvailability;
import com.campusone.aura.repository.AuraJdbcRepository.SolverRoom;
import com.campusone.aura.repository.AuraJdbcRepository.SolverSectionAvailability;
import com.campusone.aura.repository.AuraJdbcRepository.SolverTimeslot;
import com.campusone.aura.service.AuraSolverService.SolverResult;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuraSolverServiceTest {

    @Test
    void solve_sameSeedAndInputs_producesSameAssignments() {
        List<SolverRequirement> requirements = List.of(new SolverRequirement(
                id(1), id(2), id(3), id(4), 2,
                "CLASSROOM", 20, List.of()));
        List<SolverRoom> rooms = List.of(
                new SolverRoom(id(5), 30, "CLASSROOM", List.of()),
                new SolverRoom(id(6), 30, "CLASSROOM", List.of()));
        List<SolverTimeslot> timeslots = List.of(
                new SolverTimeslot(id(7), 1, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                new SolverTimeslot(id(8), 2, LocalTime.of(9, 0), LocalTime.of(10, 0)));
        AuraSolverService service = new AuraSolverService();

        SolverResult first = service.solve(
                requirements, rooms, timeslots, List.of(), List.of(), List.of(),
                List.of(), List.of(), 1, java.util.Map.of(), 42L);
        SolverResult second = service.solve(
                requirements, rooms, timeslots, List.of(), List.of(), List.of(),
                List.of(), List.of(), 1, java.util.Map.of(), 42L);

        assertThat(second.score()).isEqualTo(first.score());
        assertThat(second.assignments()).isEqualTo(first.assignments());
    }

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
                        30,
                        List.of())),
                List.of(
                        new SolverRoom(
                                UUID.fromString("90000000-0000-4000-8000-000000000005"),
                                40,
                                "CLASSROOM",
                                List.of()),
                        new SolverRoom(
                                UUID.fromString("90000000-0000-4000-8000-000000000006"),
                                40,
                                "CLASSROOM",
                                List.of())),
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
                        30,
                        List.of())),
                List.of(new SolverRoom(
                        UUID.fromString("90000000-0000-4000-8000-000000000015"),
                        40,
                        "CLASSROOM",
                        List.of())),
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
                List.of(),
                1);

        assertThat(result.assignments()).hasSize(1);
        assertThat(result.assignments().getFirst().timeslotId())
                .isEqualTo(availableTimeslot);
        assertThat(result.score()).startsWith("0hard");
    }

    @Test
    void solve_respectsSectionUnavailableTimeslotAsHardConstraint() {
        UUID instructor = UUID.fromString(
                "90000000-0000-4000-8000-000000000021");
        UUID section = UUID.fromString(
                "90000000-0000-4000-8000-000000000022");
        UUID unavailableTimeslot = UUID.fromString(
                "90000000-0000-4000-8000-000000000027");
        UUID availableTimeslot = UUID.fromString(
                "90000000-0000-4000-8000-000000000028");

        SolverResult result = new AuraSolverService().solve(
                List.of(new SolverRequirement(
                        UUID.fromString("90000000-0000-4000-8000-000000000023"),
                        UUID.fromString("90000000-0000-4000-8000-000000000024"),
                        section,
                        instructor,
                        1,
                        "CLASSROOM",
                        30,
                        List.of())),
                List.of(new SolverRoom(
                        UUID.fromString("90000000-0000-4000-8000-000000000025"),
                        40,
                        "CLASSROOM",
                        List.of())),
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
                List.of(),
                List.of(),
                List.of(new SolverSectionAvailability(
                        section,
                        unavailableTimeslot,
                        "UNAVAILABLE")),
                1);

        assertThat(result.assignments()).hasSize(1);
        assertThat(result.assignments().getFirst().timeslotId())
                .isEqualTo(availableTimeslot);
        assertThat(result.score()).startsWith("0hard");
    }

    @Test
    void solve_assignsOnlyRoomsWithEveryRequiredFacility() {
        UUID instructor = UUID.fromString(
                "90000000-0000-4000-8000-000000000031");
        UUID section = UUID.fromString(
                "90000000-0000-4000-8000-000000000032");
        UUID equippedRoom = UUID.fromString(
                "90000000-0000-4000-8000-000000000035");

        SolverResult result = new AuraSolverService().solve(
                List.of(new SolverRequirement(
                        UUID.fromString("90000000-0000-4000-8000-000000000033"),
                        UUID.fromString("90000000-0000-4000-8000-000000000034"),
                        section,
                        instructor,
                        1,
                        "CLASSROOM",
                        30,
                        List.of("PROJECTOR", "INTERNET"))),
                List.of(
                        new SolverRoom(
                                UUID.fromString("90000000-0000-4000-8000-000000000036"),
                                40,
                                "CLASSROOM",
                                List.of("PROJECTOR")),
                        new SolverRoom(
                                equippedRoom,
                                40,
                                "CLASSROOM",
                                List.of("PROJECTOR", "INTERNET"))),
                List.of(new SolverTimeslot(
                        UUID.fromString("90000000-0000-4000-8000-000000000037"),
                        1,
                        LocalTime.of(9, 0),
                        LocalTime.of(10, 0))),
                List.of(),
                List.of(),
                List.of(),
                1);

        assertThat(result.assignments()).hasSize(1);
        assertThat(result.assignments().getFirst().roomId())
                .isEqualTo(equippedRoom);
        assertThat(result.score()).startsWith("0hard");
    }

    @Test
    void solve_doesNotAssignSameInstructorToOverlappingClockRanges() {
        UUID instructor = UUID.fromString(
                "90000000-0000-4000-8000-000000000041");
        UUID firstRequirement = UUID.fromString(
                "90000000-0000-4000-8000-000000000042");
        UUID secondRequirement = UUID.fromString(
                "90000000-0000-4000-8000-000000000043");
        List<SolverTimeslot> slots = List.of(
                new SolverTimeslot(
                        UUID.fromString("90000000-0000-4000-8000-000000000044"),
                        1,
                        LocalTime.of(9, 0),
                        LocalTime.of(10, 0)),
                new SolverTimeslot(
                        UUID.fromString("90000000-0000-4000-8000-000000000045"),
                        1,
                        LocalTime.of(9, 30),
                        LocalTime.of(10, 30)),
                new SolverTimeslot(
                        UUID.fromString("90000000-0000-4000-8000-000000000046"),
                        1,
                        LocalTime.of(10, 30),
                        LocalTime.of(11, 30)));

        SolverResult result = new AuraSolverService().solve(
                List.of(
                        new SolverRequirement(
                                firstRequirement,
                                UUID.fromString("90000000-0000-4000-8000-000000000047"),
                                UUID.fromString("90000000-0000-4000-8000-000000000048"),
                                instructor,
                                1,
                                "CLASSROOM",
                                20,
                                List.of()),
                        new SolverRequirement(
                                secondRequirement,
                                UUID.fromString("90000000-0000-4000-8000-000000000049"),
                                UUID.fromString("90000000-0000-4000-8000-000000000050"),
                                instructor,
                                1,
                                "CLASSROOM",
                                20,
                                List.of())),
                List.of(
                        new SolverRoom(
                                UUID.fromString("90000000-0000-4000-8000-000000000051"),
                                30,
                                "CLASSROOM",
                                List.of()),
                        new SolverRoom(
                                UUID.fromString("90000000-0000-4000-8000-000000000052"),
                                30,
                                "CLASSROOM",
                                List.of())),
                slots,
                List.of(),
                List.of(),
                List.of(),
                1);

        assertThat(result.assignments()).hasSize(2);
        SolverTimeslot first = slots.stream()
                .filter(slot -> slot.id().equals(
                        result.assignments().get(0).timeslotId()))
                .findFirst()
                .orElseThrow();
        SolverTimeslot second = slots.stream()
                .filter(slot -> slot.id().equals(
                        result.assignments().get(1).timeslotId()))
                .findFirst()
                .orElseThrow();
        boolean overlaps = first.dayOfWeek() == second.dayOfWeek()
                && first.startsAt().isBefore(second.endsAt())
                && second.startsAt().isBefore(first.endsAt());

        assertThat(overlaps).isFalse();
        assertThat(result.score()).startsWith("0hard");
    }

    @Test
    void solve_assignsMultiSlotLessonOnlyToContiguousInstructionalBlock() {
        UUID room = id(61);
        UUID validStart = id(62);
        SolverResult result = new AuraSolverService().solve(
                List.of(requirement(
                        id(63), id(64), id(65), id(66), 1, 2,
                        List.of(), List.of(), 0, 1, null, null,
                        24, 24)),
                List.of(new SolverRoom(room, 40, "CLASSROOM", List.of())),
                List.of(
                        new SolverTimeslot(
                                validStart, 1, LocalTime.of(9, 0),
                                LocalTime.of(10, 0), 1, "INSTRUCTIONAL"),
                        new SolverTimeslot(
                                id(67), 1, LocalTime.of(10, 0),
                                LocalTime.of(11, 0), 2, "INSTRUCTIONAL"),
                        new SolverTimeslot(
                                id(68), 1, LocalTime.of(11, 0),
                                LocalTime.of(12, 0), 3, "BREAK")),
                List.of(), List.of(), List.of(), 1);

        assertThat(result.score()).startsWith("0hard");
        assertThat(result.assignments()).singleElement()
                .satisfies(assignment -> assertThat(assignment.timeslotId())
                        .isEqualTo(validStart));
    }

    @Test
    void solve_honorsFixedRoomTimeslotAndAllowedDay() {
        UUID fixedRoom = id(71);
        UUID fixedSlot = id(72);
        SolverResult result = new AuraSolverService().solve(
                List.of(requirement(
                        id(73), id(74), id(75), id(76), 1, 1,
                        List.of(2), List.of(1), 0, 1,
                        fixedRoom, fixedSlot, 24, 24)),
                List.of(
                        new SolverRoom(id(77), 40, "CLASSROOM", List.of()),
                        new SolverRoom(fixedRoom, 40, "CLASSROOM", List.of())),
                List.of(
                        new SolverTimeslot(
                                id(78), 1, LocalTime.of(9, 0),
                                LocalTime.of(10, 0), 1, "INSTRUCTIONAL"),
                        new SolverTimeslot(
                                fixedSlot, 2, LocalTime.of(9, 0),
                                LocalTime.of(10, 0), 1, "INSTRUCTIONAL")),
                List.of(), List.of(), List.of(), 1);

        assertThat(result.score()).startsWith("0hard");
        assertThat(result.assignments()).singleElement().satisfies(assignment -> {
            assertThat(assignment.roomId()).isEqualTo(fixedRoom);
            assertThat(assignment.timeslotId()).isEqualTo(fixedSlot);
        });
    }

    @Test
    void solve_respectsPerDayOccurrenceAndMinimumDaySeparationRules() {
        List<SolverTimeslot> slots = List.of(
                new SolverTimeslot(id(81), 1, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                new SolverTimeslot(id(82), 2, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                new SolverTimeslot(id(83), 3, LocalTime.of(9, 0), LocalTime.of(10, 0)));
        SolverResult result = new AuraSolverService().solve(
                List.of(requirement(
                        id(84), id(85), id(86), id(87), 2, 1,
                        List.of(), List.of(), 2, 1, null, null,
                        24, 24)),
                List.of(new SolverRoom(id(88), 40, "CLASSROOM", List.of())),
                slots, List.of(), List.of(), List.of(), 1);

        assertThat(result.score()).startsWith("0hard");
        List<Integer> assignedDays = result.assignments().stream()
                .map(assignment -> slots.stream()
                        .filter(slot -> slot.id().equals(assignment.timeslotId()))
                        .findFirst().orElseThrow().dayOfWeek())
                .sorted()
                .toList();
        assertThat(assignedDays).containsExactly(1, 3);
    }

    private SolverRequirement requirement(
            UUID requirementId,
            UUID offeringId,
            UUID sectionId,
            UUID instructorId,
            int occurrences,
            int duration,
            List<Integer> allowedDays,
            List<Integer> prohibitedDays,
            int minimumDaySeparation,
            int maximumOccurrencesPerDay,
            UUID fixedRoomId,
            UUID fixedTimeslotId,
            int instructorDailyLoad,
            int sectionDailyLoad) {
        return new SolverRequirement(
                requirementId, offeringId, sectionId, instructorId,
                occurrences, "CLASSROOM", 20, List.of(), duration,
                allowedDays, prohibitedDays, List.of(), null, null,
                minimumDaySeparation, maximumOccurrencesPerDay, true,
                fixedRoomId, fixedTimeslotId, false, "LECTURE",
                "EVERY_WEEK", 60, instructorDailyLoad, 60,
                instructorDailyLoad, 12, sectionDailyLoad, sectionDailyLoad);
    }

    private UUID id(int suffix) {
        return UUID.fromString(String.format(
                "90000000-0000-4000-8000-%012d", suffix));
    }
}
