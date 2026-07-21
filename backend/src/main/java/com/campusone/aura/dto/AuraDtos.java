package com.campusone.aura.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class AuraDtos {

    private AuraDtos() {
    }

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last) {
    }

    public record CreateTermRequest(
            @NotNull UUID universityId,
            @NotBlank @Size(max = 40) String code,
            @NotBlank @Size(max = 140) String name,
            @NotNull LocalDate startsOn,
            @NotNull LocalDate endsOn) {
    }

    public record TermResponse(
            UUID id,
            UUID universityId,
            String code,
            String name,
            LocalDate startsOn,
            LocalDate endsOn,
            String status,
            Instant createdAt,
            Instant updatedAt,
            long version) {
    }

    public record CreateProgramRequest(
            @NotNull UUID universityId,
            @NotNull UUID departmentId,
            @NotBlank @Size(max = 40) String code,
            @NotBlank @Size(max = 160) String name) {
    }

    public record ProgramResponse(
            UUID id,
            UUID universityId,
            UUID departmentId,
            String code,
            String name,
            boolean active,
            long version) {
    }

    public record CreateBatchRequest(
            @NotNull UUID programId,
            @NotBlank @Size(max = 40) String code,
            @Min(2000) @Max(2100) int admissionYear) {
    }

    public record BatchResponse(
            UUID id,
            UUID programId,
            String code,
            int admissionYear,
            boolean active,
            long version) {
    }

    public record CreateSectionRequest(
            @NotNull UUID batchId,
            @NotBlank @Size(max = 40) String code,
            @NotBlank @Size(max = 120) String displayName,
            @Min(1) @Max(1000) int studentCount) {
    }

    public record SectionResponse(
            UUID id,
            UUID batchId,
            String code,
            String displayName,
            int studentCount,
            boolean active,
            long version) {
    }

    public record CreateInstructorRequest(
            @NotNull UUID universityId,
            UUID userId,
            @NotBlank @Size(max = 140) String displayName,
            @Size(max = 254) String email,
            @Min(1) @Max(60) int maxHoursPerWeek) {
    }

    public record InstructorResponse(
            UUID id,
            UUID universityId,
            UUID userId,
            String displayName,
            String email,
            int maxHoursPerWeek,
            boolean active,
            long version) {
    }

    public record CreateRoomRequest(
            @NotNull UUID universityId,
            @Size(max = 120) String building,
            @NotBlank @Size(max = 120) String name,
            @Min(1) @Max(2000) int capacity,
            @NotBlank @Size(max = 32) String roomType,
            @Size(max = 10) List<@NotBlank @Size(max = 40) String> facilities) {
    }

    public record RoomResponse(
            UUID id,
            UUID universityId,
            String building,
            String name,
            int capacity,
            String roomType,
            List<String> facilities,
            boolean active,
            long version) {
    }

    public record CreateTimeslotRequest(
            @NotNull UUID universityId,
            @Min(1) @Max(7) int dayOfWeek,
            @NotNull LocalTime startsAt,
            @NotNull LocalTime endsAt,
            @NotBlank @Size(max = 80) String label) {
    }

    public record TimeslotResponse(
            UUID id,
            UUID universityId,
            int dayOfWeek,
            LocalTime startsAt,
            LocalTime endsAt,
            String label,
            boolean active,
            long version) {
    }

    public record CreateInstructorAvailabilityRequest(
            @NotNull UUID instructorId,
            @NotNull UUID timeslotId,
            @NotBlank @Size(max = 20) String availability,
            @Size(max = 300) String reason) {
    }

    public record CreateRoomAvailabilityRequest(
            @NotNull UUID roomId,
            @NotNull UUID timeslotId,
            @NotBlank @Size(max = 20) String availability,
            @Size(max = 300) String reason) {
    }

    public record CreateSectionAvailabilityRequest(
            @NotNull UUID sectionId,
            @NotNull UUID timeslotId,
            @NotBlank @Size(max = 20) String availability,
            @Size(max = 300) String reason) {
    }

    public record AvailabilityResponse(
            UUID id,
            UUID targetId,
            UUID timeslotId,
            int dayOfWeek,
            LocalTime startsAt,
            LocalTime endsAt,
            String label,
            String availability,
            String reason) {
    }

    public record SetupReferenceOption(
            UUID id,
            UUID parentId,
            String code,
            String name) {
    }

    public record SetupReferencesResponse(
            UUID universityId,
            List<SetupReferenceOption> departments,
            List<SetupReferenceOption> courses,
            List<SetupReferenceOption> students) {
    }

    public record CapabilitiesResponse(
            UUID universityId,
            boolean canManage) {
    }

    public record CreateOfferingRequest(
            @NotNull UUID termId,
            @NotNull UUID courseId,
            @NotNull UUID sectionId,
            @NotNull UUID instructorId,
            @Min(1) @Max(2000) int expectedStudents) {
    }

    public record OfferingResponse(
            UUID id,
            UUID termId,
            UUID courseId,
            String courseCode,
            String courseTitle,
            UUID sectionId,
            String sectionName,
            UUID instructorId,
            String instructorName,
            int expectedStudents,
            String status,
            long version) {
    }

    public record CreateMeetingRequirementRequest(
            @NotNull UUID offeringId,
            @NotBlank @Size(max = 32) String meetingType,
            @Min(1) @Max(6) int sessionsPerWeek,
            @Min(1) @Max(4) int durationSlots,
            @NotBlank @Size(max = 32) String roomType,
            @Min(1) @Max(2000) int requiredCapacity,
            @Size(max = 300) String notes,
            @Size(max = 10) List<@NotBlank @Size(max = 40) String> requiredFacilities) {
    }

    public record MeetingRequirementResponse(
            UUID id,
            UUID offeringId,
            String meetingType,
            int sessionsPerWeek,
            int durationSlots,
            String roomType,
            int requiredCapacity,
            String notes,
            List<String> requiredFacilities,
            long version) {
    }

    public record ReplaceFacilitiesRequest(
            @NotNull @Size(max = 10)
            List<@NotBlank @Size(max = 40) String> facilities) {
    }

    public record FacilitySetResponse(
            UUID targetId,
            List<String> facilities) {
    }

    public record CreateCalendarExceptionRequest(
            @NotNull UUID termId,
            @NotBlank @Size(max = 32) String exceptionType,
            @NotNull LocalDate startsOn,
            @NotNull LocalDate endsOn,
            UUID instructorId,
            UUID roomId,
            UUID sectionId,
            UUID timeslotId,
            @Size(max = 40) String facility,
            @NotBlank @Size(max = 300) String reason,
            LocalTime startsAt,
            LocalTime endsAt,
            @Size(max = 20) String recurrencePattern,
            UUID departmentId,
            UUID buildingId) {

        public CreateCalendarExceptionRequest(
                UUID termId, String exceptionType, LocalDate startsOn,
                LocalDate endsOn, UUID instructorId, UUID roomId,
                UUID sectionId, UUID timeslotId, String facility, String reason) {
            this(termId, exceptionType, startsOn, endsOn, instructorId, roomId,
                    sectionId, timeslotId, facility, reason, null, null,
                    "NONE", null, null);
        }
    }

    public record UpdateCalendarExceptionRequest(
            @NotBlank @Size(max = 32) String exceptionType,
            @NotNull LocalDate startsOn,
            @NotNull LocalDate endsOn,
            UUID instructorId,
            UUID roomId,
            UUID sectionId,
            UUID timeslotId,
            @Size(max = 40) String facility,
            @NotBlank @Size(max = 300) String reason,
            @Min(0) long version,
            LocalTime startsAt,
            LocalTime endsAt,
            @Size(max = 20) String recurrencePattern,
            UUID departmentId,
            UUID buildingId) {

        public UpdateCalendarExceptionRequest(
                String exceptionType, LocalDate startsOn, LocalDate endsOn,
                UUID instructorId, UUID roomId, UUID sectionId,
                UUID timeslotId, String facility, String reason, long version) {
            this(exceptionType, startsOn, endsOn, instructorId, roomId,
                    sectionId, timeslotId, facility, reason, version,
                    null, null, "NONE", null, null);
        }
    }

    public record CalendarExceptionResponse(
            UUID id,
            UUID termId,
            String exceptionType,
            LocalDate startsOn,
            LocalDate endsOn,
            UUID instructorId,
            UUID roomId,
            UUID sectionId,
            UUID timeslotId,
            String facility,
            String reason,
            boolean active,
            long version,
            Instant createdAt,
            Instant updatedAt,
            LocalTime startsAt,
            LocalTime endsAt,
            String recurrencePattern,
            UUID departmentId,
            UUID buildingId) {

        public CalendarExceptionResponse(
                UUID id, UUID termId, String exceptionType,
                LocalDate startsOn, LocalDate endsOn, UUID instructorId,
                UUID roomId, UUID sectionId, UUID timeslotId, String facility,
                String reason, boolean active, long version,
                Instant createdAt, Instant updatedAt) {
            this(id, termId, exceptionType, startsOn, endsOn, instructorId,
                    roomId, sectionId, timeslotId, facility, reason, active,
                    version, createdAt, updatedAt, null, null, "NONE", null, null);
        }
    }

    public record ReadinessIssue(
            String code,
            String severity,
            String message,
            String targetType,
            UUID targetId) {
    }

    public record ReadinessResponse(
            UUID termId,
            boolean ready,
            List<ReadinessIssue> issues,
            int activeRooms,
            int activeTimeslots,
            int activeInstructors,
            int activeOfferings,
            int meetingRequirements) {
    }

    public record GenerateTimetableRequest(
            @Min(1) @Max(300) Integer terminationSeconds,
            @Size(max = 500) String notes,
            @Size(max = 24) String profile,
            Long randomSeed) {

        public GenerateTimetableRequest(
                Integer terminationSeconds,
                String notes) {
            this(terminationSeconds, notes, "BALANCED", 0L);
        }
    }

    public record GenerationRunResponse(
            UUID id,
            UUID termId,
            UUID revisionId,
            String status,
            String score,
            int terminationSeconds,
            String message,
            Instant startedAt,
            Instant completedAt,
            Instant cancelledAt,
            Instant createdAt,
            String profile,
            long randomSeed,
            Integer candidateCount,
            String terminationReason) {
    }

    public record ConstraintWeightRequest(
            @NotBlank @Size(max = 100) String constraintName,
            @NotBlank @Size(max = 16) String constraintLevel,
            @Min(0) @Max(1_000_000) long weight,
            boolean active) {
    }

    public record UpsertConstraintProfileRequest(
            @NotBlank @Size(max = 24) String profile,
            @NotNull @Size(max = 50)
            List<@Valid ConstraintWeightRequest> weights) {
    }

    public record ConstraintWeightResponse(
            String constraintName,
            String constraintLevel,
            long weight,
            boolean active,
            boolean customized) {
    }

    public record ConstraintProfileResponse(
            UUID termId,
            String profile,
            List<ConstraintWeightResponse> weights) {
    }

    public record TimetableVersionResponse(
            UUID id,
            UUID termId,
            UUID generationRunId,
            int versionNumber,
            String status,
            String score,
            String notes,
            Instant createdAt,
            Instant publishedAt) {
    }

    public record SessionResponse(
            UUID id,
            UUID versionId,
            UUID offeringId,
            UUID meetingRequirementId,
            String courseCode,
            String courseTitle,
            UUID sectionId,
            String sectionName,
            UUID instructorId,
            String instructorName,
            UUID roomId,
            String roomName,
            String roomType,
            UUID timeslotId,
            int dayOfWeek,
            LocalTime startsAt,
            LocalTime endsAt,
            boolean locked,
            String source,
            int occurrenceIndex,
            int sessionsPerWeek,
            int durationSlots,
            int contiguousSlotsAvailable,
            int requiredCapacity,
            String requiredRoomType,
            int roomCapacity,
            List<String> requiredFacilities,
            List<String> roomFacilities,
            String timeslotType,
            boolean roomActive,
            boolean instructorActive,
            boolean sectionActive,
            UUID fixedRoomId,
            UUID fixedTimeslotId,
            String weekPattern,
            List<Integer> customWeeks,
            List<UUID> hardConflictOfferingIds,
            boolean instructorUnavailable,
            boolean roomUnavailable,
            boolean sectionUnavailable,
            boolean calendarException) {

        public SessionResponse {
            requiredFacilities = requiredFacilities == null
                    ? List.of() : List.copyOf(requiredFacilities);
            roomFacilities = roomFacilities == null
                    ? List.of() : List.copyOf(roomFacilities);
            customWeeks = customWeeks == null ? List.of() : List.copyOf(customWeeks);
            hardConflictOfferingIds = hardConflictOfferingIds == null
                    ? List.of() : List.copyOf(hardConflictOfferingIds);
        }

        public SessionResponse(
                UUID id,
                UUID versionId,
                UUID offeringId,
                UUID meetingRequirementId,
                String courseCode,
                String courseTitle,
                UUID sectionId,
                String sectionName,
                UUID instructorId,
                String instructorName,
                UUID roomId,
                String roomName,
                String roomType,
                UUID timeslotId,
                int dayOfWeek,
                LocalTime startsAt,
                LocalTime endsAt,
                boolean locked,
                String source) {
            this(
                    id, versionId, offeringId, meetingRequirementId,
                    courseCode, courseTitle, sectionId, sectionName,
                    instructorId, instructorName, roomId, roomName, roomType,
                    timeslotId, dayOfWeek, startsAt, endsAt, locked, source,
                    1, 1, 1, 1, 1, roomType, Integer.MAX_VALUE,
                    List.of(), List.of(), "INSTRUCTIONAL", true, true, true,
                    null, null, "EVERY_WEEK", List.of(), List.of(),
                    false, false, false, false);
        }
    }

    public record ClashResponse(
            UUID id,
            UUID versionId,
            String clashType,
            String severity,
            String message,
            UUID primarySessionId,
            UUID secondarySessionId,
            Instant detectedAt,
            Instant resolvedAt,
            String status,
            String reasonCode,
            String suggestedAction,
            List<UUID> affectedSessions,
            List<UUID> affectedStudents,
            List<UUID> affectedInstructors,
            List<UUID> affectedSections,
            List<UUID> affectedRooms) {

        public ClashResponse(
                UUID id, UUID versionId, String clashType, String severity,
                String message, UUID primarySessionId, UUID secondarySessionId,
                Instant detectedAt, Instant resolvedAt) {
            this(id, versionId, clashType, severity, message, primarySessionId,
                    secondarySessionId, detectedAt, resolvedAt, "OPEN", clashType,
                    null,
                    java.util.stream.Stream.of(primarySessionId, secondarySessionId)
                            .filter(java.util.Objects::nonNull).toList(),
                    List.of(), List.of(), List.of(), List.of());
        }

        public ClashResponse {
            affectedSessions = affectedSessions == null ? List.of() : List.copyOf(affectedSessions);
            affectedStudents = affectedStudents == null ? List.of() : List.copyOf(affectedStudents);
            affectedInstructors = affectedInstructors == null ? List.of() : List.copyOf(affectedInstructors);
            affectedSections = affectedSections == null ? List.of() : List.copyOf(affectedSections);
            affectedRooms = affectedRooms == null ? List.of() : List.copyOf(affectedRooms);
        }
    }

    public record ManualMoveRequest(
            @NotNull UUID roomId,
            @NotNull UUID timeslotId,
            @NotBlank @Size(max = 500) String reason) {
    }

    public record ManualMovePreviewRequest(
            @NotNull UUID roomId,
            @NotNull UUID timeslotId) {
    }

    public record ManualMovePreviewResponse(
            boolean allowed,
            String message,
            List<ClashResponse> clashes) {
    }

    public record CloneVersionRequest(
            @Size(max = 500) String notes) {
    }

    public record SessionSwapPreviewRequest(
            @NotNull UUID otherSessionId) {
    }

    public record SessionSwapRequest(
            @NotNull UUID otherSessionId,
            @NotBlank @Size(max = 500) String reason) {
    }

    public record SessionPinRequest(
            boolean pinned,
            @Size(max = 300) String reason) {
    }

    public record VersionSessionChange(
            UUID meetingRequirementId,
            int occurrenceIndex,
            UUID beforeSessionId,
            UUID afterSessionId,
            UUID beforeRoomId,
            UUID afterRoomId,
            UUID beforeTimeslotId,
            UUID afterTimeslotId,
            boolean assignmentChanged) {
    }

    public record VersionComparisonResponse(
            UUID baseVersionId,
            UUID comparedVersionId,
            int totalOccurrences,
            int changedOccurrences,
            int addedOccurrences,
            int removedOccurrences,
            List<VersionSessionChange> changes) {
    }

    public record AuraMetricsResponse(
            UUID termId,
            long versions,
            long publishedVersions,
            long scheduledSessions,
            long unresolvedClashes,
            long rooms,
            long timeslots,
            long offerings) {
    }

    public record ImportResultResponse(
            String type,
            int acceptedRows,
            int rejectedRows,
            List<String> errors) {
    }
}
