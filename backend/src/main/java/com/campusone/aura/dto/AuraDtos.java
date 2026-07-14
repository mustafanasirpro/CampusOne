package com.campusone.aura.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
            Instant updatedAt) {
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
            boolean active) {
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
            boolean active) {
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
            boolean active) {
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
            boolean active) {
    }

    public record CreateRoomRequest(
            @NotNull UUID universityId,
            @Size(max = 120) String building,
            @NotBlank @Size(max = 120) String name,
            @Min(1) @Max(2000) int capacity,
            @NotBlank @Size(max = 32) String roomType) {
    }

    public record RoomResponse(
            UUID id,
            UUID universityId,
            String building,
            String name,
            int capacity,
            String roomType,
            boolean active) {
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
            boolean active) {
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
            String status) {
    }

    public record CreateMeetingRequirementRequest(
            @NotNull UUID offeringId,
            @NotBlank @Size(max = 32) String meetingType,
            @Min(1) @Max(6) int sessionsPerWeek,
            @Min(1) @Max(4) int durationSlots,
            @NotBlank @Size(max = 32) String roomType,
            @Min(1) @Max(2000) int requiredCapacity,
            @Size(max = 300) String notes) {
    }

    public record MeetingRequirementResponse(
            UUID id,
            UUID offeringId,
            String meetingType,
            int sessionsPerWeek,
            int durationSlots,
            String roomType,
            int requiredCapacity,
            String notes) {
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
            @Size(max = 500) String notes) {
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
            Instant createdAt) {
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
            String source) {
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
            Instant resolvedAt) {
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
