package com.campusone.aura.service;

import com.campusone.aura.dto.AuraDtos.ReadinessIssue;
import com.campusone.aura.dto.AuraDtos.ReadinessResponse;
import com.campusone.aura.repository.AuraJdbcRepository;
import com.campusone.aura.repository.AuraJdbcRepository.TermCounts;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        prefix = "campusone.aura",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuraReadinessValidator {

    private final AuraJdbcRepository repository;

    public AuraReadinessValidator(AuraJdbcRepository repository) {
        this.repository = repository;
    }

    public ReadinessResponse validate(UUID termId) {
        TermCounts counts = repository.countsForTerm(termId);
        List<ReadinessIssue> issues = new ArrayList<>();
        requirePositive(
                issues,
                counts.rooms(),
                "AURA_NO_ROOMS",
                "Add at least one active room before generating a timetable.",
                "ROOM");
        requirePositive(
                issues,
                counts.timeslots(),
                "AURA_NO_TIMESLOTS",
                "Add active teaching timeslots before generating a timetable.",
                "TIMESLOT");
        requirePositive(
                issues,
                counts.instructors(),
                "AURA_NO_INSTRUCTORS",
                "Add instructors before generating a timetable.",
                "INSTRUCTOR");
        requirePositive(
                issues,
                counts.offerings(),
                "AURA_NO_OFFERINGS",
                "Add course offerings for this term before generating.",
                "OFFERING");
        requirePositive(
                issues,
                counts.requirements(),
                "AURA_NO_MEETING_REQUIREMENTS",
                "Each scheduled offering needs at least one meeting requirement.",
                "MEETING_REQUIREMENT");

        if (counts.requirements() > counts.rooms() * counts.timeslots()) {
            issues.add(new ReadinessIssue(
                    "AURA_CAPACITY_TOO_SMALL",
                    "ERROR",
                    "There are more required sessions than available room-timeslot combinations.",
                    "TERM",
                    termId));
        }

        return new ReadinessResponse(
                termId,
                issues.stream().noneMatch(issue -> "ERROR".equals(issue.severity())),
                List.copyOf(issues),
                counts.rooms(),
                counts.timeslots(),
                counts.instructors(),
                counts.offerings(),
                counts.requirements());
    }

    private void requirePositive(
            List<ReadinessIssue> issues,
            int count,
            String code,
            String message,
            String targetType) {
        if (count <= 0) {
            issues.add(new ReadinessIssue(
                    code,
                    "ERROR",
                    message,
                    targetType,
                    null));
        }
    }
}
