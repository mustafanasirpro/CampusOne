package com.campusone.aura.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class AuraRegistrationDtos {

    private AuraRegistrationDtos() {
    }

    public record CreateRegistrationRequest(
            @NotNull UUID termId,
            @NotNull UUID studentUserId,
            @NotNull UUID offeringId,
            @NotBlank @Size(max = 32) String registrationType,
            UUID homeSectionId,
            UUID teachingSectionId,
            UUID lectureGroupId,
            UUID labGroupId,
            UUID tutorialGroupId,
            UUID equivalentOfferingId) {
    }

    public record UpdateRegistrationRequest(
            @NotBlank @Size(max = 20) String status,
            UUID teachingSectionId,
            UUID lectureGroupId,
            UUID labGroupId,
            UUID tutorialGroupId,
            UUID equivalentOfferingId,
            long version) {
    }

    public record StudentRegistrationResponse(
            UUID id,
            UUID termId,
            UUID studentUserId,
            String studentName,
            UUID offeringId,
            String courseCode,
            String courseTitle,
            String registrationType,
            String status,
            UUID homeSectionId,
            UUID teachingSectionId,
            UUID lectureGroupId,
            UUID labGroupId,
            UUID tutorialGroupId,
            UUID equivalentOfferingId,
            Instant createdAt,
            Instant updatedAt,
            long version) {
    }

    public record PersonalTimetableEntry(
            UUID sessionId,
            UUID offeringId,
            String courseCode,
            String courseTitle,
            String instructorName,
            String sectionName,
            String roomName,
            int dayOfWeek,
            LocalTime startsAt,
            LocalTime endsAt,
            String registrationType,
            String weekPattern,
            List<Integer> customWeeks,
            boolean personalClash) {

        public PersonalTimetableEntry {
            customWeeks = customWeeks == null ? List.of() : List.copyOf(customWeeks);
        }

        public PersonalTimetableEntry(
                UUID sessionId,
                UUID offeringId,
                String courseCode,
                String courseTitle,
                String instructorName,
                String sectionName,
                String roomName,
                int dayOfWeek,
                LocalTime startsAt,
                LocalTime endsAt,
                String registrationType,
                boolean personalClash) {
            this(
                    sessionId, offeringId, courseCode, courseTitle,
                    instructorName, sectionName, roomName, dayOfWeek,
                    startsAt, endsAt, registrationType, "EVERY_WEEK",
                    List.of(), personalClash);
        }

        public PersonalTimetableEntry withPersonalClash(boolean clash) {
            return new PersonalTimetableEntry(
                    sessionId,
                    offeringId,
                    courseCode,
                    courseTitle,
                    instructorName,
                    sectionName,
                    roomName,
                    dayOfWeek,
                    startsAt,
                    endsAt,
                    registrationType,
                    weekPattern,
                    customWeeks,
                    clash);
        }
    }

    public record PersonalClash(
            UUID leftSessionId,
            UUID rightSessionId,
            String message) {
    }

    public record PersonalTimetableResponse(
            UUID termId,
            UUID studentUserId,
            List<PersonalTimetableEntry> sessions,
            List<PersonalClash> clashes) {
    }
}
