package com.campusone.aura.service;

import com.campusone.aura.dto.AuraImportDtos.ImportPreviewResponse;
import com.campusone.aura.dto.AuraImportDtos.ImportApplyResponse;
import com.campusone.aura.dto.AuraImportDtos.ImportRowIssue;
import com.campusone.aura.dto.AuraImportDtos.ImportValidationResponse;
import com.campusone.aura.dto.AuraImportDtos.ValidateImportRequest;
import com.campusone.aura.exception.AuraStateException;
import com.campusone.aura.repository.AuraImportRepository;
import com.campusone.aura.repository.AuraImportRepository.ImportJob;
import com.campusone.aura.repository.AuraImportRepository.SourceRow;
import com.campusone.aura.repository.AuraJdbcRepository;
import com.campusone.aura.repository.AuraJdbcRepository.ScopedResource;
import com.campusone.aura.dto.AuraDtos;
import com.campusone.aura.dto.AuraRegistrationDtos;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Optional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(
        prefix = "campusone.aura",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class AuraImportService {

    private static final int PREVIEW_ROW_LIMIT = 200;
    private static final int MAX_COLUMNS = 100;
    private static final int MAX_CELL_LENGTH = 500;
    private static final Set<String> ROOM_TYPES = Set.of(
            "CLASSROOM", "LAB", "LECTURE_HALL", "LECTURE_ROOM",
            "COMPUTER_LAB", "SCIENCE_LAB", "SEMINAR_ROOM", "AUDITORIUM",
            "STUDIO", "WORKSHOP", "ONLINE", "OTHER");
    private static final Set<String> FACILITIES = Set.of(
            "PROJECTOR", "SMART_BOARD", "COMPUTERS", "INTERNET",
            "LAB_EQUIPMENT", "ACCESSIBLE", "AIR_CONDITIONING",
            "VIDEO_CONFERENCING", "SPECIALIZED_SOFTWARE", "OTHER");
    private static final Set<String> MEETING_TYPES = Set.of(
            "LECTURE", "LAB", "TUTORIAL", "SEMINAR", "PROJECT",
            "WORKSHOP", "ONLINE", "HYBRID", "OTHER");
    private static final Set<String> IMPORT_TYPES = Set.of(
            "TIMETABLE", "PROGRAMS", "BATCHES", "SECTIONS", "INSTRUCTORS",
            "ROOMS", "TIMESLOTS", "AVAILABILITY", "OFFERINGS",
            "REQUIREMENTS", "CONFLICTS", "REGISTRATIONS", "EXCEPTIONS",
            "TRAVEL_RULES");
    private static final Map<String, List<String>> HEADER_ALIASES = Map.of(
            "DAY", List.of("day", "weekday", "day of week"),
            "START", List.of("start", "start time", "starts at", "time from"),
            "END", List.of("end", "end time", "ends at", "time to"),
            "COURSE", List.of("course", "course code", "subject", "module"),
            "SECTION", List.of("section", "class", "cohort", "batch"),
            "INSTRUCTOR", List.of("instructor", "teacher", "lecturer", "faculty"),
            "ROOM", List.of("room", "venue", "location", "classroom"),
            "STUDENT", List.of("student", "student email", "email", "student name"),
            "TYPE", List.of("type", "registration type", "meeting type"));
    private static final Map<String, Set<String>> TARGET_FIELDS = Map.ofEntries(
            Map.entry("PROGRAMS", Set.of("CODE", "NAME", "DEPARTMENT_CODE", "SEMESTERS")),
            Map.entry("BATCHES", Set.of("PROGRAM_CODE", "CODE", "ADMISSION_YEAR", "GRADUATION_YEAR")),
            Map.entry("SECTIONS", Set.of("PROGRAM_CODE", "BATCH_CODE", "CODE", "NAME", "STUDENTS", "SEMESTER")),
            Map.entry("INSTRUCTORS", Set.of("EMPLOYEE_CODE", "NAME", "EMAIL", "DEPARTMENT_CODE", "WEEKLY_LOAD")),
            Map.entry("ROOMS", Set.of("CODE", "NAME", "BUILDING", "CAPACITY", "ROOM_TYPE", "FACILITIES")),
            Map.entry("TIMESLOTS", Set.of("DAY", "ORDER", "LABEL", "START", "END", "TYPE")),
            Map.entry("AVAILABILITY", Set.of("RESOURCE_TYPE", "RESOURCE_CODE", "DAY", "START", "END", "AVAILABILITY", "REASON")),
            Map.entry("OFFERINGS", Set.of("CODE", "COURSE_CODE", "SECTION_CODE", "INSTRUCTOR_CODE", "ENROLLMENT", "MAX_ENROLLMENT", "PARALLEL_GROUP", "ELECTIVE_GROUP")),
            Map.entry("REQUIREMENTS", Set.of("OFFERING_CODE", "TYPE", "OCCURRENCES", "DURATION", "ROOM_TYPE", "CAPACITY", "FACILITIES", "TEACHING_GROUP")),
            Map.entry("CONFLICTS", Set.of("LEFT_OFFERING", "RIGHT_OFFERING", "SOURCE", "SEVERITY", "REASON")),
            Map.entry("REGISTRATIONS", Set.of("STUDENT_EMAIL", "OFFERING_CODE", "REGISTRATION_TYPE", "STATUS", "HOME_SECTION", "TEACHING_SECTION", "LECTURE_GROUP", "LAB_GROUP", "TUTORIAL_GROUP")),
            Map.entry("EXCEPTIONS", Set.of("TYPE", "START_DATE", "END_DATE", "TARGET_CODE", "FACILITY", "REASON")),
            Map.entry("TRAVEL_RULES", Set.of("FROM_BUILDING", "TO_BUILDING", "MINUTES", "DIFFICULTY")),
            Map.entry("TIMETABLE", Set.of(
                    "OFFERING_CODE", "COURSE", "SECTION", "INSTRUCTOR",
                    "TYPE", "OCCURRENCE", "ROOM", "DAY", "START", "END")));
    private static final Map<String, Set<String>> REQUIRED_FIELDS = Map.ofEntries(
            Map.entry("PROGRAMS", Set.of("CODE", "NAME", "DEPARTMENT_CODE")),
            Map.entry("BATCHES", Set.of("PROGRAM_CODE", "CODE", "ADMISSION_YEAR")),
            Map.entry("SECTIONS", Set.of("PROGRAM_CODE", "BATCH_CODE", "CODE", "NAME", "STUDENTS")),
            Map.entry("INSTRUCTORS", Set.of("EMPLOYEE_CODE", "NAME")),
            Map.entry("ROOMS", Set.of("CODE", "NAME", "CAPACITY", "ROOM_TYPE")),
            Map.entry("TIMESLOTS", Set.of("DAY", "ORDER", "LABEL", "START", "END", "TYPE")),
            Map.entry("AVAILABILITY", Set.of("RESOURCE_TYPE", "RESOURCE_CODE", "DAY", "START", "AVAILABILITY")),
            Map.entry("OFFERINGS", Set.of("CODE", "COURSE_CODE", "SECTION_CODE", "INSTRUCTOR_CODE", "ENROLLMENT")),
            Map.entry("REQUIREMENTS", Set.of("OFFERING_CODE", "TYPE", "OCCURRENCES", "DURATION", "ROOM_TYPE", "CAPACITY")),
            Map.entry("CONFLICTS", Set.of("LEFT_OFFERING", "RIGHT_OFFERING", "SOURCE", "SEVERITY", "REASON")),
            Map.entry("REGISTRATIONS", Set.of("STUDENT_EMAIL", "OFFERING_CODE", "REGISTRATION_TYPE")),
            Map.entry("EXCEPTIONS", Set.of("TYPE", "START_DATE", "END_DATE", "REASON")),
            Map.entry("TRAVEL_RULES", Set.of("FROM_BUILDING", "TO_BUILDING", "MINUTES", "DIFFICULTY")),
            Map.entry("TIMETABLE", Set.of(
                    "COURSE", "SECTION", "INSTRUCTOR", "ROOM", "DAY", "START")));

    private final AuraAuthorizationService authorizationService;
    private final AuraJdbcRepository auraRepository;
    private final AuraImportRepository importRepository;
    private final AuraService auraService;
    private final AuraRegistrationService registrationService;
    private final AuraClashDetector clashDetector;
    private final Clock clock;
    private final long maxBytes;
    private final int maxRows;

    public AuraImportService(
            AuraAuthorizationService authorizationService,
            AuraJdbcRepository auraRepository,
            AuraImportRepository importRepository,
            AuraService auraService,
            AuraRegistrationService registrationService,
            AuraClashDetector clashDetector,
            Clock clock,
            @Value("${AURA_IMPORT_MAX_FILE_SIZE_MB:10}") long maxFileSizeMb,
            @Value("${AURA_IMPORT_MAX_ROWS:10000}") int maxRows) {
        this.authorizationService = authorizationService;
        this.auraRepository = auraRepository;
        this.importRepository = importRepository;
        this.auraService = auraService;
        this.registrationService = registrationService;
        this.clashDetector = clashDetector;
        this.clock = clock;
        this.maxBytes = Math.max(1, Math.min(maxFileSizeMb, 50)) * 1024L * 1024L;
        this.maxRows = Math.max(1, Math.min(maxRows, 50_000));
    }

    @Transactional
    public ImportPreviewResponse preview(
            UUID userId,
            UUID termId,
            String importType,
            String requestedSource,
            MultipartFile file) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        if (!auraRepository.resourceBelongsToUniversity(
                ScopedResource.TERM, termId, universityId)) {
            throw new com.campusone.common.exception.ResourceNotFoundException("AURA term");
        }
        String normalizedType = normalizeImportType(importType);
        ValidatedUpload upload = validateUpload(file);
        ParsedImport parsed = parse(upload, requestedSource);
        Map<String, String> suggestedMapping = suggestMapping(
                normalizedType, parsed.headers());
        UUID id = UUID.randomUUID();
        List<Map<String, String>> previewRows = parsed.rows().stream()
                .limit(PREVIEW_ROW_LIMIT)
                .toList();
        importRepository.insertPreview(
                id,
                universityId,
                termId,
                userId,
                normalizedType,
                upload.format(),
                upload.filename(),
                parsed.selectedSource(),
                parsed.headers(),
                parsed.rows(),
                suggestedMapping,
                parsed.rows().size(),
                parsed.ocrRequired());
        List<String> warnings = new ArrayList<>(parsed.warnings());
        if (parsed.rows().size() > PREVIEW_ROW_LIMIT) {
            warnings.add("Only the first " + PREVIEW_ROW_LIMIT
                    + " rows are shown in the preview.");
        }
        return new ImportPreviewResponse(
                id,
                termId,
                normalizedType,
                upload.format(),
                upload.filename(),
                "PREVIEWED",
                parsed.sources(),
                parsed.selectedSource(),
                parsed.headers(),
                previewRows,
                suggestedMapping,
                parsed.rows().size(),
                parsed.rows().size() > PREVIEW_ROW_LIMIT,
                parsed.ocrRequired(),
                List.copyOf(warnings),
                clock.instant());
    }

    @Transactional
    public ImportValidationResponse validate(
            UUID userId,
            UUID jobId,
            ValidateImportRequest request) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        ImportJob job = requireJob(jobId, universityId);
        if (!Set.of("PREVIEWED", "VALIDATED").contains(job.status())) {
            throw new AuraStateException("This import can no longer be validated.");
        }
        Map<String, String> mapping = normalizeMapping(job, request.mapping());
        List<SourceRow> sourceRows = importRepository.listSourceRows(jobId);
        List<Map<String, String>> normalizedRows = sourceRows.stream()
                .map(row -> normalizeRow(row.values(), mapping))
                .toList();
        List<ImportRowIssue> issues = validateRows(job.importType(), normalizedRows);
        int rejected = (int) issues.stream()
                .filter(issue -> "ERROR".equals(issue.severity()))
                .map(ImportRowIssue::rowNumber)
                .distinct()
                .count();
        int accepted = normalizedRows.size() - rejected;
        importRepository.replaceIssues(jobId, issues);
        importRepository.markValidated(jobId, mapping, accepted, rejected);
        if (rejected == 0
                && request.saveAsProfile() != null
                && !request.saveAsProfile().isBlank()) {
            try {
                importRepository.saveMappingProfile(
                        universityId, job.importType(), request.saveAsProfile(),
                        mapping, userId);
            } catch (org.springframework.dao.DuplicateKeyException exception) {
                throw new AuraStateException(
                        "A mapping profile with that name already exists.");
            }
        }
        return new ImportValidationResponse(
                jobId,
                rejected == 0 ? "VALIDATED" : "PREVIEWED",
                mapping,
                accepted,
                rejected,
                normalizedRows.stream().limit(20).toList(),
                issues.stream().limit(200).toList());
    }

    @Transactional
    public ImportApplyResponse apply(UUID userId, UUID jobId) {
        UUID universityId = authorizationService.requireAdminUniversity(userId);
        ImportJob job = requireJob(jobId, universityId);
        if (!"VALIDATED".equals(job.status()) || job.rejectedRows() != 0
                || job.appliedMapping().isEmpty()) {
            throw new AuraStateException(
                    "Validate every import row successfully before applying it.");
        }
        if (!auraRepository.resourceBelongsToUniversity(
                ScopedResource.TERM, job.termId(), universityId)) {
            throw new com.campusone.common.exception.ResourceNotFoundException(
                    "AURA term");
        }
        List<SourceRow> sourceRows = importRepository.listSourceRows(jobId);
        if (sourceRows.isEmpty()) {
            throw new AuraStateException("The validated import has no rows to apply.");
        }
        importRepository.markApplying(jobId);
        UUID resultVersionId = null;
        if ("TIMETABLE".equals(job.importType())) {
            resultVersionId = importRepository.insertImportedVersion(
                    job.termId(), auraRepository.nextVersionNumber(job.termId()),
                    userId, "Imported timetable");
        }
        for (SourceRow sourceRow : sourceRows) {
            Map<String, String> row = normalizeRow(
                    sourceRow.values(), job.appliedMapping());
            try {
                applyRow(
                        userId, universityId, job.termId(), job.importType(),
                        row, resultVersionId);
            } catch (AuraStateException exception) {
                throw new AuraStateException(
                        "Row " + sourceRow.rowNumber() + ": "
                                + exception.getMessage());
            } catch (org.springframework.dao.DataAccessException exception) {
                throw new AuraStateException(
                        "Row " + sourceRow.rowNumber()
                                + " could not be applied because it conflicts with existing scheduling data.");
            }
        }
        if (resultVersionId != null) {
            auraRepository.replaceClashes(
                    resultVersionId,
                    clashDetector.detect(
                            auraRepository.listSessions(resultVersionId),
                            auraRepository.clashDetectionContext(resultVersionId)));
        }
        importRepository.markApplied(jobId, sourceRows.size(), resultVersionId);
        return importRepository.applyResponse(jobId);
    }

    private void applyRow(
            UUID userId,
            UUID universityId,
            UUID termId,
            String importType,
            Map<String, String> row,
            UUID resultVersionId) {
        switch (importType) {
            case "PROGRAMS" -> applyProgram(userId, universityId, row);
            case "BATCHES" -> applyBatch(userId, universityId, row);
            case "SECTIONS" -> applySection(userId, universityId, termId, row);
            case "INSTRUCTORS" -> applyInstructor(userId, universityId, row);
            case "ROOMS" -> applyRoom(userId, universityId, row);
            case "TIMESLOTS" -> applyTimeslot(userId, universityId, termId, row);
            case "AVAILABILITY" -> applyAvailability(userId, universityId, termId, row);
            case "OFFERINGS" -> applyOffering(userId, universityId, termId, row);
            case "REQUIREMENTS" -> applyRequirement(userId, termId, row);
            case "CONFLICTS" -> applyConflict(userId, termId, row);
            case "REGISTRATIONS" -> applyRegistration(userId, universityId, termId, row);
            case "EXCEPTIONS" -> applyException(userId, universityId, termId, row);
            case "TRAVEL_RULES" -> applyTravelRule(universityId, row);
            case "TIMETABLE" -> applyTimetableRow(
                    termId, row, requireValue(resultVersionId, "Timetable version"));
            default -> throw new AuraStateException("This import type cannot be applied.");
        }
    }

    private void applyProgram(UUID userId, UUID universityId, Map<String, String> row) {
        UUID departmentId = requireReference(
                importRepository.findDepartment(universityId, row.get("DEPARTMENT_CODE")),
                "Department");
        UUID id = auraService.createProgram(userId, new AuraDtos.CreateProgramRequest(
                universityId, departmentId, row.get("CODE"), row.get("NAME"))).id();
        importRepository.updateProgramImportDetails(id, optionalInteger(row, "SEMESTERS"));
    }

    private void applyBatch(UUID userId, UUID universityId, Map<String, String> row) {
        UUID programId = requireReference(
                importRepository.findProgram(universityId, row.get("PROGRAM_CODE")),
                "Program");
        UUID id = auraService.createBatch(userId, new AuraDtos.CreateBatchRequest(
                programId, row.get("CODE"), integer(row, "ADMISSION_YEAR"))).id();
        importRepository.updateBatchImportDetails(
                id, optionalInteger(row, "GRADUATION_YEAR"));
    }

    private void applySection(
            UUID userId, UUID universityId, UUID termId, Map<String, String> row) {
        UUID batchId = requireReference(importRepository.findBatch(
                universityId, row.get("PROGRAM_CODE"), row.get("BATCH_CODE")),
                "Batch");
        UUID id = auraService.createSection(userId, new AuraDtos.CreateSectionRequest(
                batchId, row.get("CODE"), row.get("NAME"),
                integer(row, "STUDENTS"))).id();
        importRepository.updateSectionImportDetails(
                id, termId, optionalInteger(row, "SEMESTER"));
    }

    private void applyInstructor(
            UUID userId, UUID universityId, Map<String, String> row) {
        String departmentCode = row.getOrDefault("DEPARTMENT_CODE", "");
        UUID departmentId = departmentCode.isBlank()
                ? null
                : requireReference(importRepository.findDepartment(
                        universityId, departmentCode), "Department");
        UUID id = auraService.createInstructor(userId,
                new AuraDtos.CreateInstructorRequest(
                        universityId, null, row.get("NAME"),
                        blankToNull(row.get("EMAIL")),
                        optionalInteger(row, "WEEKLY_LOAD") == null
                                ? 18 : optionalInteger(row, "WEEKLY_LOAD"))).id();
        importRepository.updateInstructorImportDetails(
                id, row.get("EMPLOYEE_CODE"), departmentId);
    }

    private void applyRoom(UUID userId, UUID universityId, Map<String, String> row) {
        UUID id = auraService.createRoom(userId, new AuraDtos.CreateRoomRequest(
                universityId, blankToNull(row.get("BUILDING")), row.get("NAME"),
                integer(row, "CAPACITY"), normalizedEnum(row.get("ROOM_TYPE")),
                csvValues(row.get("FACILITIES")))).id();
        importRepository.updateRoomImportDetails(id, row.get("CODE"));
    }

    private void applyTimeslot(
            UUID userId, UUID universityId, UUID termId, Map<String, String> row) {
        UUID id = auraService.createTimeslot(userId, new AuraDtos.CreateTimeslotRequest(
                universityId, day(row.get("DAY")), LocalTime.parse(row.get("START")),
                LocalTime.parse(row.get("END")), row.get("LABEL"))).id();
        importRepository.updateTimeslotImportDetails(
                id, termId, integer(row, "ORDER"), normalizedEnum(row.get("TYPE")));
    }

    private void applyAvailability(
            UUID userId, UUID universityId, UUID termId, Map<String, String> row) {
        UUID timeslotId = requireReference(importRepository.findTimeslot(
                universityId, termId, day(row.get("DAY")),
                LocalTime.parse(row.get("START"))), "Timeslot");
        String type = normalizedEnum(row.get("RESOURCE_TYPE"));
        String availability = normalizedEnum(row.get("AVAILABILITY"));
        String reason = blankToNull(row.get("REASON"));
        switch (type) {
            case "INSTRUCTOR" -> auraService.upsertInstructorAvailability(
                    userId, new AuraDtos.CreateInstructorAvailabilityRequest(
                            requireReference(importRepository.findInstructor(
                                    universityId, row.get("RESOURCE_CODE")), "Instructor"),
                            timeslotId, availability, reason));
            case "ROOM" -> auraService.upsertRoomAvailability(
                    userId, new AuraDtos.CreateRoomAvailabilityRequest(
                            requireReference(importRepository.findRoom(
                                    universityId, row.get("RESOURCE_CODE")), "Room"),
                            timeslotId, availability, reason));
            case "SECTION" -> auraService.upsertSectionAvailability(
                    userId, new AuraDtos.CreateSectionAvailabilityRequest(
                            requireReference(importRepository.findSection(
                                    universityId, termId, row.get("RESOURCE_CODE")), "Section"),
                            timeslotId, availability, reason));
            case "STUDENT" -> importRepository.upsertStudentAvailability(
                    UUID.randomUUID(), termId,
                    requireReference(importRepository.findStudent(
                            universityId, row.get("RESOURCE_CODE")), "Student"),
                    timeslotId, availability, reason);
            default -> throw new AuraStateException(
                    "Resource type must be INSTRUCTOR, ROOM, SECTION, or STUDENT.");
        }
    }

    private void applyOffering(
            UUID userId, UUID universityId, UUID termId, Map<String, String> row) {
        int expected = integer(row, "ENROLLMENT");
        Integer maximum = optionalInteger(row, "MAX_ENROLLMENT");
        if (maximum != null && maximum < expected) {
            throw new AuraStateException(
                    "Maximum enrollment cannot be less than enrollment.");
        }
        UUID id = auraService.createOffering(userId,
                new AuraDtos.CreateOfferingRequest(
                        termId,
                        requireReference(importRepository.findCourse(
                                universityId, row.get("COURSE_CODE")), "Course"),
                        requireReference(importRepository.findSection(
                                universityId, termId, row.get("SECTION_CODE")), "Section"),
                        requireReference(importRepository.findInstructor(
                                universityId, row.get("INSTRUCTOR_CODE")), "Instructor"),
                        expected)).id();
        importRepository.updateOfferingImportDetails(
                id, row.get("CODE"), maximum,
                row.get("PARALLEL_GROUP"), row.get("ELECTIVE_GROUP"));
    }

    private void applyRequirement(UUID userId, UUID termId, Map<String, String> row) {
        UUID offeringId = requireReference(importRepository.findOffering(
                termId, row.get("OFFERING_CODE")), "Offering");
        UUID id = auraService.createMeetingRequirement(userId,
                new AuraDtos.CreateMeetingRequirementRequest(
                        offeringId, normalizedEnum(row.get("TYPE")),
                        integer(row, "OCCURRENCES"), integer(row, "DURATION"),
                        normalizedEnum(row.get("ROOM_TYPE")), integer(row, "CAPACITY"),
                        null, csvValues(row.get("FACILITIES")))).id();
        importRepository.updateRequirementImportDetails(id, row.get("TEACHING_GROUP"));
    }

    private void applyConflict(UUID userId, UUID termId, Map<String, String> row) {
        UUID left = requireReference(importRepository.findOffering(
                termId, row.get("LEFT_OFFERING")), "First offering");
        UUID right = requireReference(importRepository.findOffering(
                termId, row.get("RIGHT_OFFERING")), "Second offering");
        if (left.equals(right)) {
            throw new AuraStateException("The conflict must reference two different offerings.");
        }
        importRepository.insertConflict(
                termId, left, right, normalizedEnum(row.get("SOURCE")),
                normalizedEnum(row.get("SEVERITY")), row.get("REASON"), userId);
    }

    private void applyRegistration(
            UUID userId, UUID universityId, UUID termId, Map<String, String> row) {
        UUID offeringId = requireReference(importRepository.findOffering(
                termId, row.get("OFFERING_CODE")), "Offering");
        UUID homeSection = optionalSection(universityId, termId, row.get("HOME_SECTION"));
        UUID teachingSection = optionalSection(
                universityId, termId, row.get("TEACHING_SECTION"));
        AuraRegistrationDtos.CreateRegistrationRequest request =
                new AuraRegistrationDtos.CreateRegistrationRequest(
                        termId,
                        requireReference(importRepository.findStudent(
                                universityId, row.get("STUDENT_EMAIL")), "Student"),
                        offeringId, normalizedEnum(row.get("REGISTRATION_TYPE")),
                        homeSection, teachingSection,
                        optionalGroup(offeringId, "LECTURE", row.get("LECTURE_GROUP")),
                        optionalGroup(offeringId, "LAB", row.get("LAB_GROUP")),
                        optionalGroup(offeringId, "TUTORIAL", row.get("TUTORIAL_GROUP")),
                        null);
        var created = registrationService.createRegistration(userId, request);
        String status = row.getOrDefault("STATUS", "");
        if (!status.isBlank() && !"ACTIVE".equals(normalizedEnum(status))) {
            registrationService.updateRegistration(
                    userId, created.id(), new AuraRegistrationDtos.UpdateRegistrationRequest(
                            normalizedEnum(status), teachingSection,
                            request.lectureGroupId(), request.labGroupId(),
                            request.tutorialGroupId(), null, created.version()));
        }
    }

    private void applyException(
            UUID userId, UUID universityId, UUID termId, Map<String, String> row) {
        String type = normalizedEnum(row.get("TYPE"));
        String target = row.getOrDefault("TARGET_CODE", "");
        UUID instructorId = "INSTRUCTOR_ABSENCE".equals(type)
                ? requireReference(importRepository.findInstructor(universityId, target),
                        "Instructor") : null;
        UUID roomId = "ROOM_CLOSURE".equals(type)
                ? requireReference(importRepository.findRoom(universityId, target), "Room")
                : null;
        UUID sectionId = "SECTION_RESTRICTION".equals(type)
                ? requireReference(importRepository.findSection(
                        universityId, termId, target), "Section") : null;
        UUID timeslotId = "TIMESLOT_CANCELLATION".equals(type)
                ? requireReference(importRepository.findTimeslotByLabel(
                        universityId, termId, target), "Timeslot") : null;
        auraService.createCalendarException(userId,
                new AuraDtos.CreateCalendarExceptionRequest(
                        termId, type, LocalDate.parse(row.get("START_DATE")),
                        LocalDate.parse(row.get("END_DATE")), instructorId, roomId,
                        sectionId, timeslotId, blankToNull(row.get("FACILITY")),
                        row.get("REASON")));
    }

    private void applyTravelRule(UUID universityId, Map<String, String> row) {
        importRepository.upsertTravelRule(
                universityId, row.get("FROM_BUILDING"), row.get("TO_BUILDING"),
                integer(row, "MINUTES"), normalizedEnum(row.get("DIFFICULTY")));
    }

    private void applyTimetableRow(
            UUID termId, Map<String, String> row, UUID versionId) {
        String reference = firstNonBlank(row.get("OFFERING_CODE"), row.get("COURSE"));
        String meetingType = firstNonBlank(row.get("TYPE"), "LECTURE");
        UUID requirementId = requireReference(importRepository.findRequirement(
                termId, reference, normalizedEnum(meetingType)), "Meeting requirement");
        UUID roomId = requireReference(importRepository.findRoomForTerm(
                termId, row.get("ROOM")), "Room");
        UUID timeslotId = requireReference(importRepository.findTimeslotForTerm(
                termId, day(row.get("DAY")), LocalTime.parse(row.get("START"))),
                "Timeslot");
        importRepository.insertImportedSession(
                versionId, requirementId, roomId, timeslotId,
                optionalInteger(row, "OCCURRENCE") == null
                        ? 1 : optionalInteger(row, "OCCURRENCE"));
    }

    private ImportJob requireJob(UUID jobId, UUID universityId) {
        ImportJob job = importRepository.findJob(jobId)
                .orElseThrow(() -> new com.campusone.common.exception.ResourceNotFoundException(
                        "AURA import"));
        if (!universityId.equals(job.universityId())) {
            throw new com.campusone.common.exception.ResourceNotFoundException("AURA import");
        }
        return job;
    }

    private Map<String, String> normalizeMapping(
            ImportJob job,
            Map<String, String> rawMapping) {
        Set<String> allowed = TARGET_FIELDS.getOrDefault(job.importType(), Set.of());
        Map<String, String> normalized = new LinkedHashMap<>();
        rawMapping.forEach((rawTarget, rawHeader) -> {
            String target = rawTarget == null ? "" : rawTarget.trim()
                    .toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
            String header = rawHeader == null ? "" : rawHeader.trim();
            if (!allowed.contains(target)) {
                throw new AuraStateException("The mapping contains an unsupported target field.");
            }
            if (!job.headers().contains(header)) {
                throw new AuraStateException("The mapping refers to a column that is not in this import.");
            }
            if (normalized.put(target, header) != null) {
                throw new AuraStateException("Each target field may be mapped only once.");
            }
        });
        Set<String> missing = new LinkedHashSet<>(
                REQUIRED_FIELDS.getOrDefault(job.importType(), Set.of()));
        missing.removeAll(normalized.keySet());
        if (!missing.isEmpty()) {
            throw new AuraStateException(
                    "Map every required field: " + String.join(", ", missing) + ".");
        }
        return Map.copyOf(normalized);
    }

    private Map<String, String> normalizeRow(
            Map<String, String> source,
            Map<String, String> mapping) {
        Map<String, String> row = new LinkedHashMap<>();
        mapping.forEach((target, header) -> row.put(
                target, safeCell(source.getOrDefault(header, ""))));
        return Map.copyOf(row);
    }

    private List<ImportRowIssue> validateRows(
            String importType,
            List<Map<String, String>> rows) {
        List<ImportRowIssue> issues = new ArrayList<>();
        Set<String> required = REQUIRED_FIELDS.getOrDefault(importType, Set.of());
        Set<String> seenKeys = new LinkedHashSet<>();
        for (int index = 0; index < rows.size(); index++) {
            int rowNumber = index + 1;
            Map<String, String> row = rows.get(index);
            for (String field : required) {
                if (row.getOrDefault(field, "").isBlank()) {
                    issues.add(issue(rowNumber, field, "REQUIRED",
                            field.replace('_', ' ') + " is required."));
                }
            }
            validateInteger(rowNumber, row, issues,
                    "SEMESTERS", 1, 20);
            validateInteger(rowNumber, row, issues,
                    "ADMISSION_YEAR", 2000, 2100);
            validateInteger(rowNumber, row, issues,
                    "GRADUATION_YEAR", 2000, 2120);
            validateInteger(rowNumber, row, issues,
                    "STUDENTS", 1, 2000);
            validateInteger(rowNumber, row, issues,
                    "SEMESTER", 1, 20);
            validateInteger(rowNumber, row, issues,
                    "WEEKLY_LOAD", 1, 60);
            validateInteger(rowNumber, row, issues,
                    "CAPACITY", 1, 5000);
            validateInteger(rowNumber, row, issues,
                    "ORDER", 1, 100);
            validateInteger(rowNumber, row, issues,
                    "ENROLLMENT", 1, 5000);
            validateInteger(rowNumber, row, issues,
                    "MAX_ENROLLMENT", 1, 5000);
            validateInteger(rowNumber, row, issues,
                    "OCCURRENCES", 1, 12);
            validateInteger(rowNumber, row, issues,
                    "OCCURRENCE", 1, 12);
            validateInteger(rowNumber, row, issues,
                    "DURATION", 1, 12);
            validateInteger(rowNumber, row, issues,
                    "MINUTES", 0, 240);
            validateDay(rowNumber, row, issues);
            validateTime(rowNumber, row, issues, "START");
            validateTime(rowNumber, row, issues, "END");
            validateDate(rowNumber, row, issues, "START_DATE");
            validateDate(rowNumber, row, issues, "END_DATE");
            validateChronology(rowNumber, row, issues);
            validateImportValues(importType, rowNumber, row, issues);
            String logicalKey = logicalKey(importType, row);
            if (logicalKey != null && !seenKeys.add(logicalKey)) {
                issues.add(issue(rowNumber, null, "DUPLICATE_ROW",
                        "This row duplicates an earlier scheduling record in the file."));
            }
        }
        return List.copyOf(issues);
    }

    private void validateChronology(
            int rowNumber,
            Map<String, String> row,
            List<ImportRowIssue> issues) {
        String start = row.getOrDefault("START", "");
        String end = row.getOrDefault("END", "");
        try {
            if (!start.isBlank() && !end.isBlank()
                    && !LocalTime.parse(start).isBefore(LocalTime.parse(end))) {
                issues.add(issue(rowNumber, "END", "INVALID_RANGE",
                        "End time must be after start time."));
            }
        } catch (java.time.format.DateTimeParseException ignored) {
            // The field-specific error already explains the invalid time.
        }
        String startsOn = row.getOrDefault("START_DATE", "");
        String endsOn = row.getOrDefault("END_DATE", "");
        try {
            if (!startsOn.isBlank() && !endsOn.isBlank()
                    && LocalDate.parse(startsOn).isAfter(LocalDate.parse(endsOn))) {
                issues.add(issue(rowNumber, "END_DATE", "INVALID_RANGE",
                        "End date must be on or after start date."));
            }
        } catch (java.time.format.DateTimeParseException ignored) {
            // The field-specific error already explains the invalid date.
        }
        Integer expected = parsedInteger(row.get("ENROLLMENT"));
        Integer maximum = parsedInteger(row.get("MAX_ENROLLMENT"));
        if (expected != null && maximum != null && maximum < expected) {
            issues.add(issue(rowNumber, "MAX_ENROLLMENT", "INVALID_RANGE",
                    "Maximum enrollment cannot be less than enrollment."));
        }
    }

    private void validateImportValues(
            String importType,
            int row,
            Map<String, String> values,
            List<ImportRowIssue> issues) {
        switch (importType) {
            case "ROOMS" -> {
                validateEnum(row, values, issues, "ROOM_TYPE", ROOM_TYPES);
                validateCsvEnums(row, values, issues, "FACILITIES", FACILITIES);
            }
            case "TIMESLOTS" -> validateEnum(
                    row, values, issues, "TYPE", Set.of("INSTRUCTIONAL", "BREAK"));
            case "AVAILABILITY" -> {
                validateEnum(row, values, issues, "RESOURCE_TYPE",
                        Set.of("INSTRUCTOR", "ROOM", "SECTION", "STUDENT"));
                validateEnum(row, values, issues, "AVAILABILITY",
                        Set.of("AVAILABLE", "UNAVAILABLE", "AVOID", "PREFERRED"));
            }
            case "REQUIREMENTS" -> {
                validateEnum(row, values, issues, "TYPE", MEETING_TYPES);
                validateEnum(row, values, issues, "ROOM_TYPE", ROOM_TYPES);
                validateCsvEnums(row, values, issues, "FACILITIES", FACILITIES);
            }
            case "CONFLICTS" -> {
                validateEnum(row, values, issues, "SOURCE", Set.of(
                        "REPEATER_REGISTRATION", "ELECTIVE_REGISTRATION",
                        "SHARED_STUDENTS", "PROGRAM_POLICY", "MANUAL"));
                validateEnum(row, values, issues, "SEVERITY", Set.of("HARD", "MEDIUM"));
            }
            case "REGISTRATIONS" -> {
                validateEnum(row, values, issues, "REGISTRATION_TYPE", Set.of(
                        "PRIMARY_SECTION", "REPEATER", "ELECTIVE", "CROSS_SECTION",
                        "IMPROVEMENT", "MAKEUP", "MANUAL", "TRANSFERRED"));
                validateOptionalEnum(row, values, issues, "STATUS", Set.of(
                        "ACTIVE", "PENDING", "DROPPED", "COMPLETED", "REJECTED"));
                String email = values.getOrDefault("STUDENT_EMAIL", "");
                if (!email.isBlank() && !email.matches(
                        "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                    issues.add(issue(row, "STUDENT_EMAIL", "INVALID_EMAIL",
                            "Student email must be a valid email address."));
                }
            }
            case "EXCEPTIONS" -> {
                validateEnum(row, values, issues, "TYPE", Set.of(
                        "HOLIDAY", "NON_TEACHING_DAY", "UNIVERSITY_EVENT",
                        "INSTRUCTOR_ABSENCE", "ROOM_CLOSURE", "SECTION_RESTRICTION",
                        "TIMESLOT_CANCELLATION", "FACILITY_OUTAGE"));
                String type = normalizedEnum(values.get("TYPE"));
                if (Set.of("INSTRUCTOR_ABSENCE", "ROOM_CLOSURE",
                        "SECTION_RESTRICTION", "TIMESLOT_CANCELLATION")
                        .contains(type)
                        && values.getOrDefault("TARGET_CODE", "").isBlank()) {
                    issues.add(issue(row, "TARGET_CODE", "REQUIRED",
                            "Affected resource is required for this exception type."));
                }
                if ("FACILITY_OUTAGE".equals(type)) {
                    validateEnum(row, values, issues, "FACILITY", FACILITIES);
                } else {
                    validateOptionalEnum(row, values, issues, "FACILITY", FACILITIES);
                }
            }
            case "TRAVEL_RULES" -> validateEnum(
                    row, values, issues, "DIFFICULTY",
                    Set.of("NORMAL", "DIFFICULT", "IMPOSSIBLE"));
            case "TIMETABLE" -> validateOptionalEnum(
                    row, values, issues, "TYPE", MEETING_TYPES);
            default -> { }
        }
    }

    private void validateEnum(
            int row,
            Map<String, String> values,
            List<ImportRowIssue> issues,
            String field,
            Set<String> allowed) {
        String value = values.getOrDefault(field, "");
        if (value.isBlank() || !allowed.contains(normalizedEnum(value))) {
            issues.add(issue(row, field, "INVALID_VALUE",
                    field.replace('_', ' ') + " has an unsupported value."));
        }
    }

    private void validateOptionalEnum(
            int row,
            Map<String, String> values,
            List<ImportRowIssue> issues,
            String field,
            Set<String> allowed) {
        if (!values.getOrDefault(field, "").isBlank()) {
            validateEnum(row, values, issues, field, allowed);
        }
    }

    private void validateCsvEnums(
            int row,
            Map<String, String> values,
            List<ImportRowIssue> issues,
            String field,
            Set<String> allowed) {
        for (String value : csvValues(values.get(field))) {
            if (!allowed.contains(value)) {
                issues.add(issue(row, field, "INVALID_VALUE",
                        field.replace('_', ' ') + " contains an unsupported value."));
                return;
            }
        }
    }

    private Integer parsedInteger(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String logicalKey(String importType, Map<String, String> row) {
        List<String> fields = switch (importType) {
            case "PROGRAMS", "BATCHES", "ROOMS", "INSTRUCTORS" -> List.of("CODE");
            case "SECTIONS" -> List.of("PROGRAM_CODE", "BATCH_CODE", "CODE");
            case "TIMESLOTS" -> List.of("DAY", "START", "END");
            case "OFFERINGS" -> List.of("CODE");
            case "REQUIREMENTS" -> List.of("OFFERING_CODE", "TYPE", "TEACHING_GROUP");
            case "CONFLICTS" -> List.of("LEFT_OFFERING", "RIGHT_OFFERING");
            case "REGISTRATIONS" -> List.of("STUDENT_EMAIL", "OFFERING_CODE");
            case "TRAVEL_RULES" -> List.of("FROM_BUILDING", "TO_BUILDING");
            case "TIMETABLE" -> List.of("COURSE", "SECTION", "TYPE", "OCCURRENCE");
            default -> List.of();
        };
        if (fields.isEmpty()) return null;
        String key = fields.stream()
                .map(field -> normalizeHeader(row.getOrDefault(field, "")))
                .collect(java.util.stream.Collectors.joining("|"));
        return key.replace("|", "").isBlank() ? null : key;
    }

    private void validateInteger(
            int row,
            Map<String, String> values,
            List<ImportRowIssue> issues,
            String field,
            int minimum,
            int maximum) {
        String raw = values.getOrDefault(field, "");
        if (raw.isBlank()) return;
        try {
            int value = Integer.parseInt(raw);
            if (value < minimum || value > maximum) throw new NumberFormatException();
        } catch (NumberFormatException exception) {
            issues.add(issue(row, field, "INVALID_NUMBER",
                    field.replace('_', ' ') + " must be between "
                            + minimum + " and " + maximum + "."));
        }
    }

    private void validateDay(
            int row,
            Map<String, String> values,
            List<ImportRowIssue> issues) {
        String raw = values.getOrDefault("DAY", "");
        if (raw.isBlank()) return;
        if (!Set.of("1", "2", "3", "4", "5", "6", "7", "MONDAY",
                "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY",
                "SUNDAY").contains(raw.toUpperCase(Locale.ROOT))) {
            issues.add(issue(row, "DAY", "INVALID_DAY",
                    "Use a weekday name or a number from 1 to 7."));
        }
    }

    private void validateTime(
            int row,
            Map<String, String> values,
            List<ImportRowIssue> issues,
            String field) {
        String raw = values.getOrDefault(field, "");
        if (raw.isBlank()) return;
        try {
            LocalTime.parse(raw);
        } catch (java.time.format.DateTimeParseException exception) {
            issues.add(issue(row, field, "INVALID_TIME",
                    field + " must use a 24-hour time such as 09:30."));
        }
    }

    private void validateDate(
            int row,
            Map<String, String> values,
            List<ImportRowIssue> issues,
            String field) {
        String raw = values.getOrDefault(field, "");
        if (raw.isBlank()) return;
        try {
            LocalDate.parse(raw);
        } catch (java.time.format.DateTimeParseException exception) {
            issues.add(issue(row, field, "INVALID_DATE",
                    field.replace('_', ' ') + " must use YYYY-MM-DD."));
        }
    }

    private ImportRowIssue issue(
            int row,
            String field,
            String code,
            String message) {
        return new ImportRowIssue(row, field, code, message, "ERROR");
    }

    private int integer(Map<String, String> row, String field) {
        return Integer.parseInt(row.get(field));
    }

    private Integer optionalInteger(Map<String, String> row, String field) {
        String value = row.getOrDefault(field, "");
        return value.isBlank() ? null : Integer.valueOf(value);
    }

    private int day(String raw) {
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "MONDAY" -> 1;
            case "TUESDAY" -> 2;
            case "WEDNESDAY" -> 3;
            case "THURSDAY" -> 4;
            case "FRIDAY" -> 5;
            case "SATURDAY" -> 6;
            case "SUNDAY" -> 7;
            default -> Integer.parseInt(raw);
        };
    }

    private String normalizedEnum(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_').replace(' ', '_');
    }

    private List<String> csvValues(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split("[,;]"))
                .map(this::normalizedEnum)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private UUID optionalSection(
            UUID universityId, UUID termId, String value) {
        return value == null || value.isBlank()
                ? null
                : requireReference(importRepository.findSection(
                        universityId, termId, value), "Section");
    }

    private UUID optionalGroup(UUID offeringId, String type, String value) {
        return value == null || value.isBlank()
                ? null
                : requireReference(importRepository.findTeachingGroup(
                        offeringId, type, value), type + " group");
    }

    private <T> T requireReference(Optional<T> value, String label) {
        return value.orElseThrow(() -> new AuraStateException(label + " was not found."));
    }

    private <T> T requireValue(T value, String label) {
        if (value == null) throw new AuraStateException(label + " was not created.");
        return value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String firstNonBlank(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }

    private ParsedImport parse(ValidatedUpload upload, String requestedSource) {
        try {
            return switch (upload.format()) {
                case "CSV" -> parseCsv(upload.bytes());
                case "XLS", "XLSX" -> parseWorkbook(upload.bytes(), requestedSource);
                case "PDF" -> parsePdf(upload.bytes());
                default -> throw new AuraStateException("Select a supported import file.");
            };
        } catch (AuraStateException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new AuraStateException(
                    "The import file could not be read. Check that it is not damaged or password protected.");
        }
    }

    private ParsedImport parseCsv(byte[] bytes) throws IOException {
        if (containsNul(bytes)) {
            throw new AuraStateException("The CSV file contains unsupported binary data.");
        }
        try (Reader reader = new InputStreamReader(
                new ByteArrayInputStream(stripUtf8Bom(bytes)), StandardCharsets.UTF_8);
                CSVParser parser = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setIgnoreEmptyLines(true)
                        .setTrim(true)
                        .get()
                        .parse(reader)) {
            List<String> headers = validateHeaders(parser.getHeaderNames());
            List<Map<String, String>> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                if (rows.size() >= maxRows) {
                    throw new AuraStateException(
                            "The import contains more than " + maxRows + " rows.");
                }
                Map<String, String> row = new LinkedHashMap<>();
                for (String header : headers) {
                    row.put(header, safeCell(record.isMapped(header)
                            ? record.get(header)
                            : ""));
                }
                if (row.values().stream().anyMatch(value -> !value.isBlank())) {
                    rows.add(Map.copyOf(row));
                }
            }
            return parsed(headers, rows, List.of("CSV"), "CSV");
        }
    }

    private ParsedImport parseWorkbook(byte[] bytes, String requestedSource)
            throws IOException {
        try (Workbook workbook = WorkbookFactory.create(
                new ByteArrayInputStream(bytes))) {
            if (workbook.getNumberOfSheets() > 50) {
                throw new AuraStateException("The workbook contains too many sheets.");
            }
            List<String> sources = new ArrayList<>();
            for (int index = 0; index < workbook.getNumberOfSheets(); index++) {
                if (!workbook.isSheetHidden(index)
                        && !workbook.isSheetVeryHidden(index)) {
                    sources.add(workbook.getSheetName(index));
                }
            }
            if (sources.isEmpty()) {
                throw new AuraStateException("The workbook has no visible sheets.");
            }
            String selected = requestedSource == null || requestedSource.isBlank()
                    ? sources.getFirst()
                    : requestedSource.trim();
            if (!sources.contains(selected)) {
                throw new AuraStateException("Select a valid workbook sheet.");
            }
            Sheet sheet = workbook.getSheet(selected);
            Row headerRow = firstNonEmptyRow(sheet);
            if (headerRow == null) {
                throw new AuraStateException("The selected sheet is empty.");
            }
            int columnCount = Math.min(headerRow.getLastCellNum(), MAX_COLUMNS);
            List<String> rawHeaders = new ArrayList<>();
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            for (int column = 0; column < columnCount; column++) {
                rawHeaders.add(safeCell(cellText(
                        headerRow.getCell(column), formatter)));
            }
            List<String> headers = validateHeaders(rawHeaders);
            List<Map<String, String>> rows = new ArrayList<>();
            for (int index = headerRow.getRowNum() + 1;
                    index <= sheet.getLastRowNum();
                    index++) {
                if (rows.size() >= maxRows) {
                    throw new AuraStateException(
                            "The import contains more than " + maxRows + " rows.");
                }
                Row workbookRow = sheet.getRow(index);
                if (workbookRow == null) continue;
                Map<String, String> row = new LinkedHashMap<>();
                for (int column = 0; column < headers.size(); column++) {
                    row.put(headers.get(column), safeCell(cellText(
                            workbookRow.getCell(column), formatter)));
                }
                if (row.values().stream().anyMatch(value -> !value.isBlank())) {
                    rows.add(Map.copyOf(row));
                }
            }
            return new ParsedImport(
                    headers,
                    List.copyOf(rows),
                    List.copyOf(sources),
                    selected,
                    false,
                    List.of());
        }
    }

    private ParsedImport parsePdf(byte[] bytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted()) {
                throw new AuraStateException("Password-protected PDFs cannot be imported.");
            }
            if (document.getNumberOfPages() > 200) {
                throw new AuraStateException("The PDF contains too many pages.");
            }
            String text = new PDFTextStripper().getText(document);
            if (text == null || text.isBlank()) {
                return new ParsedImport(
                        List.of(),
                        List.of(),
                        List.of("All pages"),
                        "All pages",
                        true,
                        List.of("OCR_REQUIRED: This PDF does not contain readable text."));
            }
            List<String[]> lines = text.lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .map(line -> line.split("(?:\\t+|\\s{2,}|\\s*\\|\\s*)"))
                    .filter(parts -> parts.length > 1)
                    .limit(maxRows + 1L)
                    .toList();
            if (lines.isEmpty()) {
                throw new AuraStateException(
                        "No timetable table could be identified in the PDF text.");
            }
            List<String> headers = validateHeaders(Arrays.stream(lines.getFirst())
                    .map(String::trim)
                    .toList());
            List<Map<String, String>> rows = new ArrayList<>();
            for (int index = 1; index < lines.size(); index++) {
                if (rows.size() >= maxRows) {
                    throw new AuraStateException(
                            "The import contains more than " + maxRows + " rows.");
                }
                Map<String, String> row = new LinkedHashMap<>();
                String[] values = lines.get(index);
                for (int column = 0; column < headers.size(); column++) {
                    row.put(headers.get(column), safeCell(
                            column < values.length ? values[column] : ""));
                }
                rows.add(Map.copyOf(row));
            }
            return parsed(headers, rows, List.of("All pages"), "All pages");
        }
    }

    private Row firstNonEmptyRow(Sheet sheet) {
        for (Row row : sheet) {
            if (row != null && row.cellIterator().hasNext()) return row;
        }
        return null;
    }

    private String cellText(Cell cell, DataFormatter formatter) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.FORMULA) {
            return cell.getCellFormula();
        }
        return formatter.formatCellValue(cell);
    }

    private ParsedImport parsed(
            List<String> headers,
            List<Map<String, String>> rows,
            List<String> sources,
            String selectedSource) {
        return new ParsedImport(
                headers,
                List.copyOf(rows),
                sources,
                selectedSource,
                false,
                List.of());
    }

    private List<String> validateHeaders(List<String> rawHeaders) {
        if (rawHeaders.isEmpty() || rawHeaders.size() > MAX_COLUMNS) {
            throw new AuraStateException("The import must contain 1 to 100 columns.");
        }
        List<String> headers = new ArrayList<>();
        Set<String> unique = new LinkedHashSet<>();
        for (int index = 0; index < rawHeaders.size(); index++) {
            String header = safeCell(rawHeaders.get(index));
            if (header.isBlank()) header = "Column " + (index + 1);
            String candidate = header;
            int suffix = 2;
            while (!unique.add(candidate.toLowerCase(Locale.ROOT))) {
                candidate = header + " " + suffix++;
            }
            headers.add(candidate);
        }
        return List.copyOf(headers);
    }

    private Map<String, String> suggestMapping(
            String importType,
            List<String> headers) {
        Map<String, String> mapping = new LinkedHashMap<>();
        for (String target : TARGET_FIELDS.getOrDefault(importType, Set.of())) {
            List<String> aliases = new ArrayList<>();
            aliases.add(target.toLowerCase(Locale.ROOT).replace('_', ' '));
            aliases.addAll(HEADER_ALIASES.getOrDefault(target, List.of()));
            headers.stream()
                    .filter(header -> aliases.contains(normalizeHeader(header)))
                    .findFirst()
                    .ifPresent(header -> mapping.put(target, header));
        }
        return Map.copyOf(mapping);
    }

    private ValidatedUpload validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AuraStateException("Choose a non-empty import file.");
        }
        if (file.getSize() > maxBytes) {
            throw new AuraStateException("The import file exceeds the configured size limit.");
        }
        String original = file.getOriginalFilename() == null
                ? "import"
                : file.getOriginalFilename();
        String filename = original.replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1).trim();
        if (filename.isBlank() || filename.length() > 255
                || filename.chars().anyMatch(Character::isISOControl)) {
            throw new AuraStateException("The import filename is not valid.");
        }
        String extension = extension(filename);
        String format = switch (extension) {
            case "csv" -> "CSV";
            case "xlsx" -> "XLSX";
            case "xls" -> "XLS";
            case "pdf" -> "PDF";
            default -> throw new AuraStateException(
                    "Only CSV, XLSX, XLS, and text-based PDF files are supported.");
        };
        try {
            byte[] bytes = file.getBytes();
            validateMagic(format, bytes);
            return new ValidatedUpload(filename, format, bytes);
        } catch (IOException exception) {
            throw new AuraStateException("The import file could not be read.");
        }
    }

    private void validateMagic(String format, byte[] bytes) {
        boolean valid = switch (format) {
            case "PDF" -> startsWith(bytes, new byte[] {'%', 'P', 'D', 'F', '-'});
            case "XLSX" -> startsWith(bytes, new byte[] {'P', 'K'});
            case "XLS" -> startsWith(bytes, new byte[] {
                    (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0});
            case "CSV" -> !containsNul(bytes);
            default -> false;
        };
        if (!valid) {
            throw new AuraStateException(
                    "The file content does not match its filename extension.");
        }
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) return false;
        for (int index = 0; index < prefix.length; index++) {
            if (value[index] != prefix[index]) return false;
        }
        return true;
    }

    private boolean containsNul(byte[] value) {
        for (byte current : value) {
            if (current == 0) return true;
        }
        return false;
    }

    private byte[] stripUtf8Bom(byte[] value) {
        if (value.length >= 3
                && value[0] == (byte) 0xEF
                && value[1] == (byte) 0xBB
                && value[2] == (byte) 0xBF) {
            return Arrays.copyOfRange(value, 3, value.length);
        }
        return value;
    }

    private String extension(String filename) {
        int separator = filename.lastIndexOf('.');
        return separator < 0 ? "" : filename.substring(separator + 1)
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeImportType(String raw) {
        String type = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (!IMPORT_TYPES.contains(type)) {
            throw new AuraStateException("Select a valid AURA import type.");
        }
        return type;
    }

    private String normalizeHeader(String value) {
        return value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    private String safeCell(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.length() > MAX_CELL_LENGTH) {
            throw new AuraStateException(
                    "An import cell exceeds the 500-character safety limit.");
        }
        if (value.chars().anyMatch(character ->
                Character.isISOControl(character)
                        && character != '\t'
                        && character != '\n'
                        && character != '\r')) {
            throw new AuraStateException("The import contains unsupported control characters.");
        }
        return value;
    }

    private record ValidatedUpload(String filename, String format, byte[] bytes) {
    }

    private record ParsedImport(
            List<String> headers,
            List<Map<String, String>> rows,
            List<String> sources,
            String selectedSource,
            boolean ocrRequired,
            List<String> warnings) {
    }
}
