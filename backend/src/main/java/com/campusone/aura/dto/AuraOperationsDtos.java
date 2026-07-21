package com.campusone.aura.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** DTOs for AURA administration features that span more than one setup aggregate. */
public final class AuraOperationsDtos {

    private AuraOperationsDtos() {
    }

    public record MutationResponse(
            UUID id,
            String resourceType,
            boolean active,
            long version,
            Instant updatedAt) {
    }

    public record ActiveStateRequest(
            boolean active,
            @Min(0) long version,
            @NotBlank @Size(max = 300) String reason) {
    }

    public record UpdateTermRequest(
            @NotBlank @Size(max = 40) String code,
            @NotBlank @Size(max = 140) String name,
            @NotNull LocalDate startsOn,
            @NotNull LocalDate endsOn,
            @NotBlank @Size(max = 64) String timezone,
            @NotBlank @Size(max = 24) String status,
            @Min(0) long version) {
    }

    public record UpdateProgramRequest(
            @NotNull UUID departmentId,
            @NotBlank @Size(max = 40) String code,
            @NotBlank @Size(max = 160) String name,
            @Min(1) @Max(20) int numberOfSemesters,
            boolean active,
            @Min(0) long version) {
    }

    public record UpdateBatchRequest(
            @NotBlank @Size(max = 40) String code,
            @Min(2000) @Max(2100) int admissionYear,
            @Min(2000) @Max(2120) Integer expectedGraduationYear,
            boolean active,
            @Min(0) long version) {
    }

    public record UpdateSectionRequest(
            @NotNull UUID batchId,
            UUID termId,
            @NotBlank @Size(max = 40) String code,
            @NotBlank @Size(max = 120) String displayName,
            @Min(1) @Max(2000) int studentCount,
            @Min(1) @Max(20) Integer semesterNumber,
            @Min(1) @Max(24) int hardDailyLoad,
            @Min(1) @Max(24) int preferredDailyLoad,
            UUID homeBuildingId,
            boolean active,
            @Min(0) long version) {
    }

    public record UpdateInstructorRequest(
            UUID userId,
            UUID departmentId,
            @Size(max = 60) String employeeCode,
            @NotBlank @Size(max = 140) String displayName,
            @Size(max = 254) String email,
            @Min(1) @Max(60) int maxHoursPerWeek,
            @Min(1) @Max(24) int hardDailyLoad,
            @Min(1) @Max(60) int preferredWeeklyLoad,
            @Min(1) @Max(24) int preferredDailyLoad,
            @Min(1) @Max(12) int maximumConsecutiveSlots,
            UUID homeBuildingId,
            boolean active,
            @Min(0) long version) {
    }

    public record UpdateRoomRequest(
            UUID buildingId,
            @Size(max = 60) String roomCode,
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 120) String displayName,
            @Min(1) @Max(2000) int capacity,
            @NotBlank @Size(max = 32) String roomType,
            @Size(max = 10) List<@NotBlank @Size(max = 40) String> facilities,
            boolean active,
            @Min(0) long version) {
    }

    public record UpdateTimeslotRequest(
            UUID termId,
            @Min(1) @Max(7) int dayOfWeek,
            @NotNull LocalTime startsAt,
            @NotNull LocalTime endsAt,
            @NotBlank @Size(max = 80) String label,
            @Min(1) @Max(100) Integer slotOrder,
            @NotBlank @Size(max = 20) String slotType,
            boolean active,
            @Min(0) long version) {
    }

    public record UpdateOfferingRequest(
            @NotNull UUID courseId,
            @NotNull UUID sectionId,
            @NotNull UUID instructorId,
            @Size(max = 80) String offeringCode,
            @Min(1) @Max(2000) int expectedStudents,
            @Min(1) @Max(2000) Integer maximumEnrollment,
            boolean combinedSections,
            @Size(max = 80) String parallelGroup,
            @Size(max = 80) String electiveGroup,
            @Size(max = 500) String notes,
            @NotBlank @Size(max = 20) String status,
            @Min(0) long version) {
    }

    public record UpdateMeetingRequirementRequest(
            @NotBlank @Size(max = 32) String meetingType,
            @Min(1) @Max(6) int sessionsPerWeek,
            @Min(1) @Max(4) int durationSlots,
            @NotBlank @Size(max = 32) String roomType,
            @Min(1) @Max(2000) int requiredCapacity,
            @Size(max = 300) String notes,
            List<@Min(1) @Max(7) Integer> allowedDays,
            List<@Min(1) @Max(7) Integer> prohibitedDays,
            List<@Min(1) @Max(7) Integer> preferredDays,
            LocalTime preferredStartTime,
            LocalTime preferredEndTime,
            @Min(0) @Max(6) int minimumDaySeparation,
            @Min(1) @Max(6) int maximumOccurrencesPerDay,
            boolean sameRoomPreferred,
            UUID fixedRoomId,
            UUID fixedTimeslotId,
            boolean pinned,
            UUID teachingGroupId,
            UUID linkedRequirementId,
            boolean lectureBeforeLinked,
            @NotBlank @Size(max = 24) String weekPattern,
            List<@Min(1) @Max(53) Integer> customWeeks,
            @Size(max = 10) List<@NotBlank @Size(max = 40) String> requiredFacilities,
            boolean active,
            @Min(0) long version) {
    }

    public record CreateBuildingRequest(
            @NotBlank @Size(max = 40) String code,
            @NotBlank @Size(max = 120) String name,
            @Min(0) @Max(240) int minimumTransitionMinutes) {
    }

    public record UpdateBuildingRequest(
            @NotBlank @Size(max = 40) String code,
            @NotBlank @Size(max = 120) String name,
            @Min(0) @Max(240) int minimumTransitionMinutes,
            boolean active,
            @Min(0) long version) {
    }

    public record BuildingResponse(
            UUID id,
            String code,
            String name,
            int minimumTransitionMinutes,
            boolean active,
            long version) {
    }

    public record UpsertTeachingGroupRequest(
            @NotNull UUID offeringId,
            @NotBlank @Size(max = 20) String groupType,
            @NotBlank @Size(max = 80) String code,
            @NotBlank @Size(max = 140) String displayName,
            @Min(1) @Max(2000) Integer capacity,
            boolean active,
            Long version) {
    }

    public record TeachingGroupResponse(
            UUID id,
            UUID offeringId,
            String groupType,
            String code,
            String displayName,
            Integer capacity,
            boolean active,
            long version) {
    }

    public record UpsertStudentAvailabilityRequest(
            @NotNull UUID termId,
            @NotNull UUID studentUserId,
            @NotNull UUID timeslotId,
            @NotBlank @Size(max = 20) String availability,
            @Size(max = 300) String reason) {
    }

    public record StudentAvailabilityResponse(
            UUID id,
            UUID termId,
            UUID studentUserId,
            UUID timeslotId,
            String availability,
            String reason,
            long version) {
    }

    public record UpsertOfferingConflictRequest(
            @NotNull UUID termId,
            @NotNull UUID leftOfferingId,
            @NotNull UUID rightOfferingId,
            @NotBlank @Size(max = 40) String source,
            @NotBlank @Size(max = 16) String severity,
            @NotBlank @Size(max = 300) String reason,
            boolean active,
            Long version) {
    }

    public record OfferingConflictResponse(
            UUID id,
            UUID termId,
            UUID leftOfferingId,
            UUID rightOfferingId,
            String source,
            String severity,
            String reason,
            boolean active,
            long version) {
    }

    public record UpsertTravelRuleRequest(
            @NotBlank @Size(max = 120) String fromBuilding,
            @NotBlank @Size(max = 120) String toBuilding,
            @Min(0) @Max(240) int minutes,
            @NotBlank @Size(max = 16) String difficulty,
            boolean active,
            Long version) {
    }

    public record TravelRuleResponse(
            UUID id,
            String fromBuilding,
            String toBuilding,
            int minutes,
            String difficulty,
            boolean active,
            long version) {
    }

    public record CalendarScopeRequest(
            @NotNull UUID termId,
            @NotBlank @Size(max = 32) String exceptionType,
            @NotNull LocalDate startsOn,
            @NotNull LocalDate endsOn,
            LocalTime startsAt,
            LocalTime endsAt,
            @NotBlank @Size(max = 20) String recurrencePattern,
            UUID instructorId,
            UUID roomId,
            UUID sectionId,
            UUID timeslotId,
            UUID departmentId,
            UUID buildingId,
            @Size(max = 40) String facility,
            @NotBlank @Size(max = 300) String reason) {
    }

    public record AuditEventResponse(
            UUID id,
            UUID termId,
            UUID actorUserId,
            String actorName,
            String action,
            String targetType,
            UUID targetId,
            String summary,
            Map<String, Object> metadata,
            UUID correlationId,
            String result,
            Instant createdAt) {
    }

    public record AnalyticsResponse(
            UUID termId,
            UUID versionId,
            Map<String, Double> roomUtilization,
            Map<String, Double> buildingUtilization,
            Map<String, Integer> instructorLoads,
            Map<String, Integer> sectionLoads,
            Map<String, Long> clashesByType,
            Map<String, Long> sessionsByDay,
            double averageRoomCapacityUtilization,
            long unresolvedClashes,
            long impossibleRequirements,
            long repairPlans,
            double averageRepairDisruption) {
    }

    public record ScopedTimetableResponse(
            UUID termId,
            UUID versionId,
            String scopeType,
            UUID scopeId,
            String scopeLabel,
            List<AuraDtos.SessionResponse> sessions) {
        public ScopedTimetableResponse {
            sessions = sessions == null ? List.of() : List.copyOf(sessions);
        }
    }

    public record RepairPreviewRequest(
            UUID clashId,
            UUID sessionId,
            @NotBlank @Size(max = 500) String reason) {
    }

    public record RepairMove(
            UUID sessionId,
            UUID originalRoomId,
            UUID originalTimeslotId,
            UUID proposedRoomId,
            UUID proposedTimeslotId,
            int affectedStudents,
            int disruptionScore) {
    }

    public record RepairImpact(
            int sessionsMoved,
            int studentsAffected,
            int instructorsAffected,
            int sectionsAffected,
            int roomChanges,
            int dayChanges,
            int timeChanges,
            int newClashes,
            int clashesResolved,
            int disruptionScore) {
    }

    public record RepairPlanResponse(
            UUID id,
            UUID sourceVersionId,
            UUID draftVersionId,
            String triggerType,
            UUID triggerId,
            String status,
            boolean feasible,
            String message,
            String previewToken,
            List<RepairMove> proposedMoves,
            @Valid RepairImpact impact,
            Instant expiresAt,
            Instant appliedAt) {
        public RepairPlanResponse {
            proposedMoves = proposedMoves == null ? List.of() : List.copyOf(proposedMoves);
        }
    }

    public record ApplyRepairRequest(
            @NotBlank @Size(max = 64) String previewToken) {
    }
}
