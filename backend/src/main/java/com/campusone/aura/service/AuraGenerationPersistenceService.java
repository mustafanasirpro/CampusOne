package com.campusone.aura.service;

import com.campusone.aura.exception.AuraStateException;
import com.campusone.aura.repository.AuraJdbcRepository;
import com.campusone.aura.repository.AuraJdbcRepository.DetectedClash;
import com.campusone.aura.repository.AuraJdbcRepository.RunPersistenceState;
import com.campusone.aura.repository.AuraJdbcRepository.SolverAssignment;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(
        prefix = "campusone.aura",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuraGenerationPersistenceService {

    private final AuraJdbcRepository repository;
    private final AuraClashDetector clashDetector;

    public AuraGenerationPersistenceService(
            AuraJdbcRepository repository,
            AuraClashDetector clashDetector) {
        this.repository = repository;
        this.clashDetector = clashDetector;
    }

    @Transactional
    public UUID persistCompletedRun(
            UUID runId,
            UUID termId,
            UUID userId,
            String score,
            String notes,
            List<SolverAssignment> assignments,
            int candidateCount,
            String terminationReason) {
        RunPersistenceState runState = repository.lockRunForPersistence(
                runId, termId);
        if (!"RUNNING".equals(runState.status())) {
            throw new AuraStateException(
                    "This generation run is no longer active.");
        }
        if (!runState.currentRevision()) {
            throw new AuraStateException(
                    "Scheduling data changed while the timetable was being generated. Start a new generation run.");
        }

        UUID versionId = repository.insertVersion(
                UUID.randomUUID(),
                termId,
                runId,
                repository.nextVersionNumber(termId),
                score,
                notes,
                userId);
        for (SolverAssignment assignment : assignments) {
            repository.insertSession(
                    UUID.randomUUID(), versionId, assignment, "SOLVER");
        }

        List<DetectedClash> detected = clashDetector.detect(
                repository.listSessions(versionId),
                repository.clashDetectionContext(versionId));
        repository.replaceClashes(versionId, detected);
        boolean completed = repository.markRunCompleted(
                runId,
                score,
                "Timetable generated with "
                        + assignments.size()
                        + " scheduled sessions.",
                candidateCount,
                terminationReason);
        if (!completed) {
            throw new AuraStateException(
                    "This generation run could not be completed safely.");
        }
        return versionId;
    }
}
