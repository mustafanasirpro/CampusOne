package com.campusone.aura.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusone.aura.repository.AuraJdbcRepository.SolverRequirement;
import com.campusone.aura.repository.AuraJdbcRepository.SolverRoom;
import com.campusone.aura.repository.AuraJdbcRepository.SolverTimeslot;
import com.campusone.aura.service.AuraSolverService.SolverResult;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuraSolverPerformanceTest {

    @Test
    void solve_boundedConflictFreeScenarios_assignsEveryOccurrence() {
        for (int size : benchmarkSizes()) {
            BenchmarkInput input = input(size);
            long usedBefore = usedMemory();
            Instant started = Instant.now();

            SolverResult result = new AuraSolverService().solve(
                    input.requirements(), input.rooms(), input.timeslots(),
                    List.of(), List.of(), List.of(), List.of(), List.of(),
                    Integer.getInteger("aura.benchmark.seconds", 1));

            long elapsedMillis = Duration.between(started, Instant.now()).toMillis();
            long memoryDelta = Math.max(0L, usedMemory() - usedBefore);
            System.out.printf(
                    "AURA_BENCHMARK occurrences=%d elapsedMs=%d score=%s memoryDeltaBytes=%d%n",
                    size, elapsedMillis, result.score(), memoryDelta);
            assertThat(result.assignments()).hasSize(size);
            assertThat(result.score()).startsWith("0hard");
        }
    }

    private List<Integer> benchmarkSizes() {
        return java.util.Arrays.stream(System.getProperty(
                        "aura.benchmark.sizes", "300").split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(Integer::parseInt)
                .peek(size -> {
                    if (size < 1 || size > 5_000) {
                        throw new IllegalArgumentException(
                                "Benchmark sizes must be between 1 and 5000.");
                    }
                })
                .toList();
    }

    private BenchmarkInput input(int occurrenceCount) {
        int timeslotCount = Math.min(100, Math.max(20, occurrenceCount / 20));
        int roomCount = (int) Math.ceil((double) occurrenceCount / timeslotCount);
        List<SolverRoom> rooms = new ArrayList<>(roomCount);
        for (int index = 0; index < roomCount; index++) {
            rooms.add(new SolverRoom(
                    id("room", index), 80, "CLASSROOM", List.of()));
        }
        List<SolverTimeslot> timeslots = new ArrayList<>(timeslotCount);
        for (int index = 0; index < timeslotCount; index++) {
            int slotsPerDay = (int) Math.ceil(timeslotCount / 5.0);
            int day = Math.min(5, index / slotsPerDay + 1);
            int dayIndex = index % slotsPerDay;
            LocalTime start = LocalTime.of(7, 0).plusMinutes(dayIndex * 30L);
            timeslots.add(new SolverTimeslot(
                    id("timeslot", index), day, start, start.plusMinutes(30),
                    dayIndex + 1, "INSTRUCTIONAL"));
        }
        List<SolverRequirement> requirements = new ArrayList<>(occurrenceCount);
        for (int index = 0; index < occurrenceCount; index++) {
            requirements.add(new SolverRequirement(
                    id("requirement", index), id("offering", index),
                    id("section", index), id("instructor", index),
                    1, "CLASSROOM", 20, List.of()));
        }
        return new BenchmarkInput(requirements, rooms, timeslots);
    }

    private UUID id(String type, int index) {
        return UUID.nameUUIDFromBytes(
                ("aura-benchmark:" + type + ":" + index)
                        .getBytes(StandardCharsets.UTF_8));
    }

    private long usedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private record BenchmarkInput(
            List<SolverRequirement> requirements,
            List<SolverRoom> rooms,
            List<SolverTimeslot> timeslots) {
    }
}
