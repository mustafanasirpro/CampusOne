package com.campusone.aura.service;

import com.campusone.aura.dto.AuraRegistrationDtos;
import com.campusone.aura.dto.AuraRegistrationDtos.PersonalClash;
import com.campusone.aura.dto.AuraRegistrationDtos.PersonalTimetableEntry;
import com.campusone.aura.dto.AuraRegistrationDtos.PersonalTimetableResponse;
import com.campusone.aura.dto.AuraRegistrationDtos.StudentRegistrationResponse;
import com.campusone.aura.dto.AuraDtos.TermResponse;
import com.campusone.aura.exception.AuraStateException;
import com.campusone.aura.repository.AuraJdbcRepository;
import com.campusone.aura.repository.AuraJdbcRepository.ScopedResource;
import com.campusone.aura.repository.AuraRegistrationRepository;
import com.campusone.common.exception.ResourceNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(
        prefix = "campusone.aura",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuraRegistrationService {

    private static final Set<String> REGISTRATION_TYPES = Set.of(
            "PRIMARY_SECTION",
            "REPEATER",
            "ELECTIVE",
            "CROSS_SECTION",
            "IMPROVEMENT",
            "MAKEUP",
            "MANUAL",
            "TRANSFERRED");
    private static final Set<String> REGISTRATION_STATUSES = Set.of(
            "ACTIVE", "PENDING", "DROPPED", "COMPLETED", "REJECTED");

    private final AuraAuthorizationService authorizationService;
    private final AuraJdbcRepository auraRepository;
    private final AuraRegistrationRepository registrationRepository;

    public AuraRegistrationService(
            AuraAuthorizationService authorizationService,
            AuraJdbcRepository auraRepository,
            AuraRegistrationRepository registrationRepository) {
        this.authorizationService = authorizationService;
        this.auraRepository = auraRepository;
        this.registrationRepository = registrationRepository;
    }

    @Transactional
    public StudentRegistrationResponse createRegistration(
            UUID actorUserId,
            AuraRegistrationDtos.CreateRegistrationRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(actorUserId);
        requireTerm(request.termId(), universityId);
        if (!registrationRepository.studentBelongsToUniversity(
                request.studentUserId(), universityId)) {
            throw new ResourceNotFoundException("Student");
        }
        validateOffering(request.offeringId(), request.termId());
        validateOptionalOffering(request.equivalentOfferingId(), request.termId());
        validateSections(request, universityId);
        validateGroups(
                request.offeringId(),
                request.lectureGroupId(),
                request.labGroupId(),
                request.tutorialGroupId());
        String registrationType = normalized(
                request.registrationType(), REGISTRATION_TYPES, "registration type");
        UUID id = UUID.randomUUID();
        try {
            registrationRepository.insertRegistration(
                    id, universityId, actorUserId, request, registrationType);
        } catch (DataIntegrityViolationException exception) {
            throw new AuraStateException(
                    "This student already has an active registration for the offering.");
        }
        return requireRegistration(id);
    }

    @Transactional(readOnly = true)
    public List<StudentRegistrationResponse> listRegistrations(
            UUID actorUserId,
            UUID termId,
            UUID studentUserId) {
        UUID universityId = authorizationService.requireAdminUniversity(actorUserId);
        requireTerm(termId, universityId);
        if (studentUserId != null
                && !registrationRepository.studentBelongsToUniversity(
                        studentUserId, universityId)) {
            throw new ResourceNotFoundException("Student");
        }
        return registrationRepository.listRegistrations(termId, studentUserId);
    }

    @Transactional
    public StudentRegistrationResponse updateRegistration(
            UUID actorUserId,
            UUID registrationId,
            AuraRegistrationDtos.UpdateRegistrationRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(actorUserId);
        StudentRegistrationResponse existing = requireRegistration(registrationId);
        requireTerm(existing.termId(), universityId);
        validateOptionalOffering(request.equivalentOfferingId(), existing.termId());
        validateSection(
                request.teachingSectionId(), existing.termId(), universityId);
        validateGroups(
                existing.offeringId(),
                request.lectureGroupId(),
                request.labGroupId(),
                request.tutorialGroupId());
        String status = normalized(
                request.status(), REGISTRATION_STATUSES, "registration status");
        if (!registrationRepository.updateRegistration(
                registrationId, request, status)) {
            throw new AuraStateException(
                    "The registration changed while you were editing it. Refresh and try again.");
        }
        return requireRegistration(registrationId);
    }

    @Transactional(readOnly = true)
    public List<StudentRegistrationResponse> listMyRegistrations(
            UUID studentUserId,
            UUID termId) {
        UUID universityId = authorizationService.requireUniversity(studentUserId);
        requireTerm(termId, universityId);
        return registrationRepository.listRegistrations(termId, studentUserId);
    }

    @Transactional(readOnly = true)
    public List<TermResponse> listAvailableTerms(UUID studentUserId) {
        UUID universityId = authorizationService.requireUniversity(studentUserId);
        return auraRepository.listPublishedTerms(universityId);
    }

    @Transactional(readOnly = true)
    public PersonalTimetableResponse personalTimetable(
            UUID studentUserId,
            UUID termId) {
        UUID universityId = authorizationService.requireUniversity(studentUserId);
        requireTerm(termId, universityId);
        return buildPersonalTimetable(termId, studentUserId);
    }

    @Transactional(readOnly = true)
    public byte[] personalTimetableCalendar(UUID studentUserId, UUID termId) {
        UUID universityId = authorizationService.requireUniversity(studentUserId);
        requireTerm(termId, universityId);
        TermResponse term = auraRepository.findTerm(termId)
                .orElseThrow(() -> new ResourceNotFoundException("AURA term"));
        String timezone = auraRepository.termTimezone(termId);
        if (timezone == null
                || !timezone.matches("[A-Za-z0-9_+.-]+(?:/[A-Za-z0-9_+.-]+)*")) {
            timezone = "UTC";
        }
        PersonalTimetableResponse timetable = buildPersonalTimetable(
                termId, studentUserId);
        StringBuilder calendar = new StringBuilder()
                .append("BEGIN:VCALENDAR\r\n")
                .append("VERSION:2.0\r\n")
                .append("PRODID:-//CampusOne//AURA Personal Timetable//EN\r\n")
                .append("CALSCALE:GREGORIAN\r\n")
                .append("METHOD:PUBLISH\r\n")
                .append("X-WR-CALNAME:CampusOne AURA timetable\r\n")
                .append("X-WR-TIMEZONE:").append(timezone).append("\r\n");
        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
        for (PersonalTimetableEntry entry : timetable.sessions()) {
            LocalDate first = term.startsOn().with(TemporalAdjusters.nextOrSame(
                    DayOfWeek.of(entry.dayOfWeek())));
            for (LocalDate date = first;
                    !date.isAfter(term.endsOn());
                    date = date.plusWeeks(1)) {
                int week = (int) ChronoUnit.WEEKS.between(
                        term.startsOn(), date) + 1;
                if (!occursInWeek(entry, week)) {
                    continue;
                }
                calendar.append("BEGIN:VEVENT\r\n")
                        .append("UID:").append(entry.sessionId()).append('-')
                        .append(week).append("@campusone.dev\r\n")
                        .append("DTSTAMP:").append(stamp).append("\r\n")
                        .append("DTSTART;TZID=").append(timezone).append(':')
                        .append(icsDateTime(date, entry.startsAt())).append("\r\n")
                        .append("DTEND;TZID=").append(timezone).append(':')
                        .append(icsDateTime(date, entry.endsAt())).append("\r\n")
                        .append("SUMMARY:").append(escapeIcs(
                                entry.courseCode() + " · " + entry.courseTitle()))
                        .append("\r\n")
                        .append("LOCATION:").append(escapeIcs(entry.roomName()))
                        .append("\r\n")
                        .append("DESCRIPTION:").append(escapeIcs(
                                entry.instructorName() + " · " + entry.sectionName()))
                        .append("\r\nEND:VEVENT\r\n");
            }
        }
        calendar.append("END:VCALENDAR\r\n");
        return calendar.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public PersonalTimetableResponse adminPersonalTimetable(
            UUID actorUserId,
            UUID termId,
            UUID studentUserId) {
        UUID universityId = authorizationService.requireAdminUniversity(actorUserId);
        requireTerm(termId, universityId);
        if (!registrationRepository.studentBelongsToUniversity(
                studentUserId, universityId)) {
            throw new ResourceNotFoundException("Student");
        }
        return buildPersonalTimetable(termId, studentUserId);
    }

    private PersonalTimetableResponse buildPersonalTimetable(
            UUID termId,
            UUID studentUserId) {
        List<PersonalTimetableEntry> entries = registrationRepository
                .personalTimetable(termId, studentUserId);
        List<PersonalClash> clashes = new ArrayList<>();
        Set<UUID> clashingSessionIds = new HashSet<>();
        for (int leftIndex = 0; leftIndex < entries.size(); leftIndex++) {
            PersonalTimetableEntry left = entries.get(leftIndex);
            for (int rightIndex = leftIndex + 1;
                    rightIndex < entries.size();
                    rightIndex++) {
                PersonalTimetableEntry right = entries.get(rightIndex);
                if (overlaps(left, right)) {
                    clashingSessionIds.add(left.sessionId());
                    clashingSessionIds.add(right.sessionId());
                    clashes.add(new PersonalClash(
                            left.sessionId(),
                            right.sessionId(),
                            left.courseCode() + " overlaps " + right.courseCode() + "."));
                }
            }
        }
        List<PersonalTimetableEntry> markedEntries = entries.stream()
                .map(entry -> clashingSessionIds.contains(entry.sessionId())
                        ? entry.withPersonalClash(true)
                        : entry)
                .toList();
        return new PersonalTimetableResponse(
                termId, studentUserId, markedEntries, List.copyOf(clashes));
    }

    private boolean overlaps(
            PersonalTimetableEntry left,
            PersonalTimetableEntry right) {
        return left.dayOfWeek() == right.dayOfWeek()
                && left.startsAt().isBefore(right.endsAt())
                && right.startsAt().isBefore(left.endsAt())
                && weekPatternsOverlap(left, right);
    }

    private boolean weekPatternsOverlap(
            PersonalTimetableEntry left,
            PersonalTimetableEntry right) {
        if ("EVERY_WEEK".equals(left.weekPattern())
                || "EVERY_WEEK".equals(right.weekPattern())) {
            return true;
        }
        if (("ODD_WEEK".equals(left.weekPattern())
                    && "EVEN_WEEK".equals(right.weekPattern()))
                || ("EVEN_WEEK".equals(left.weekPattern())
                    && "ODD_WEEK".equals(right.weekPattern()))) {
            return false;
        }
        if ("CUSTOM_WEEK_SET".equals(left.weekPattern())
                && "CUSTOM_WEEK_SET".equals(right.weekPattern())) {
            return left.customWeeks().stream().anyMatch(right.customWeeks()::contains);
        }
        PersonalTimetableEntry custom = "CUSTOM_WEEK_SET".equals(left.weekPattern())
                ? left : right;
        PersonalTimetableEntry alternating = custom == left ? right : left;
        boolean odd = "ODD_WEEK".equals(alternating.weekPattern());
        return custom.customWeeks().stream()
                .anyMatch(week -> (week % 2 == 1) == odd);
    }

    private boolean occursInWeek(PersonalTimetableEntry entry, int week) {
        return switch (entry.weekPattern()) {
            case "ODD_WEEK" -> week % 2 == 1;
            case "EVEN_WEEK" -> week % 2 == 0;
            case "CUSTOM_WEEK_SET" -> entry.customWeeks().contains(week);
            default -> true;
        };
    }

    private String icsDateTime(LocalDate date, java.time.LocalTime time) {
        return date.format(DateTimeFormatter.BASIC_ISO_DATE)
                + 'T' + time.format(DateTimeFormatter.ofPattern("HHmmss"));
    }

    private String escapeIcs(String raw) {
        return (raw == null ? "" : raw)
                .replace("\\", "\\\\")
                .replace("\r", "")
                .replace("\n", "\\n")
                .replace(",", "\\,")
                .replace(";", "\\;");
    }

    private void validateSections(
            AuraRegistrationDtos.CreateRegistrationRequest request,
            UUID universityId) {
        validateSection(request.homeSectionId(), request.termId(), universityId);
        validateSection(request.teachingSectionId(), request.termId(), universityId);
    }

    private void validateSection(
            UUID sectionId,
            UUID termId,
            UUID universityId) {
        if (!registrationRepository.sectionBelongsToTermUniversity(
                sectionId, termId, universityId)) {
            throw new ResourceNotFoundException("Section");
        }
    }

    private void validateGroups(
            UUID offeringId,
            UUID lectureGroupId,
            UUID labGroupId,
            UUID tutorialGroupId) {
        validateGroup(lectureGroupId, offeringId, "LECTURE");
        validateGroup(labGroupId, offeringId, "LAB");
        validateGroup(tutorialGroupId, offeringId, "TUTORIAL");
    }

    private void validateGroup(UUID groupId, UUID offeringId, String type) {
        if (!registrationRepository.groupMatchesOfferingAndType(
                groupId, offeringId, type)) {
            throw new ResourceNotFoundException(type + " teaching group");
        }
    }

    private void validateOffering(UUID offeringId, UUID termId) {
        if (!registrationRepository.offeringBelongsToTerm(offeringId, termId)) {
            throw new ResourceNotFoundException("Course offering");
        }
    }

    private void validateOptionalOffering(UUID offeringId, UUID termId) {
        if (offeringId != null) {
            validateOffering(offeringId, termId);
        }
    }

    private void requireTerm(UUID termId, UUID universityId) {
        if (!auraRepository.resourceBelongsToUniversity(
                ScopedResource.TERM, termId, universityId)) {
            throw new ResourceNotFoundException("AURA term");
        }
    }

    private StudentRegistrationResponse requireRegistration(UUID id) {
        return registrationRepository.findRegistration(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registration"));
    }

    private String normalized(String raw, Set<String> allowed, String label) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(value)) {
            throw new AuraStateException("Select a valid " + label + ".");
        }
        return value;
    }
}
