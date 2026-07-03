package com.campusone.internship.service;

import com.campusone.common.exception.ResourceNotFoundException;
import com.campusone.internship.dto.request.CreateInternshipRequest;
import com.campusone.internship.dto.request.InternshipSort;
import com.campusone.internship.dto.request.UpdateInternshipRequest;
import com.campusone.internship.dto.response.InternshipDetailResponse;
import com.campusone.internship.dto.response.InternshipPageResponse;
import com.campusone.internship.dto.response.InternshipSummaryResponse;
import com.campusone.internship.dto.response.SavedInternshipResponse;
import com.campusone.internship.entity.Internship;
import com.campusone.internship.entity.InternshipStatus;
import com.campusone.internship.entity.InternshipType;
import com.campusone.internship.entity.SavedInternship;
import com.campusone.internship.entity.SavedInternshipId;
import com.campusone.internship.entity.WorkMode;
import com.campusone.internship.exception.InternshipConflictException;
import com.campusone.internship.mapper.InternshipMapper;
import com.campusone.internship.repository.InternshipRepository;
import com.campusone.internship.repository.SavedInternshipRepository;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InternshipService {

    private final InternshipRepository internshipRepository;
    private final SavedInternshipRepository savedRepository;
    private final UserRepository userRepository;
    private final InternshipMapper internshipMapper;
    private final Clock clock;

    public InternshipService(
            InternshipRepository internshipRepository,
            SavedInternshipRepository savedRepository,
            UserRepository userRepository,
            InternshipMapper internshipMapper,
            Clock clock) {
        this.internshipRepository = internshipRepository;
        this.savedRepository = savedRepository;
        this.userRepository = userRepository;
        this.internshipMapper = internshipMapper;
        this.clock = clock;
    }

    @Transactional
    public InternshipDetailResponse createInternship(
            UUID userId,
            CreateInternshipRequest request) {
        validateFutureDeadline(request.deadline());
        User poster = requireUser(userId);
        Internship internship = internshipRepository.save(new Internship(
                poster,
                request.title(),
                request.companyName(),
                request.description(),
                request.location(),
                request.internshipType(),
                request.workMode(),
                request.paid(),
                request.stipendAmount(),
                request.currency(),
                request.applyUrl(),
                request.deadline()));
        return internshipMapper.toDetail(internship, false, true);
    }

    @Transactional(readOnly = true)
    public InternshipPageResponse listInternships(
            UUID viewerUserId,
            InternshipStatus status,
            InternshipType internshipType,
            WorkMode workMode,
            Boolean paid,
            String search,
            int page,
            int size,
            InternshipSort sort) {
        Page<Internship> internships =
                internshipRepository.findVisibleInternships(
                        status,
                        internshipType,
                        workMode,
                        paid,
                        toSearchPattern(search),
                        PageRequest.of(page, size, sort.toSort()));
        return toPage(internships, viewerUserId);
    }

    @Transactional(readOnly = true)
    public InternshipDetailResponse getInternship(
            UUID internshipId,
            UUID viewerUserId) {
        Internship internship = requireInternship(internshipId);
        return toDetail(internship, viewerUserId);
    }

    @Transactional(readOnly = true)
    public InternshipPageResponse listMyInternships(
            UUID userId,
            InternshipStatus status,
            InternshipType internshipType,
            WorkMode workMode,
            Boolean paid,
            String search,
            int page,
            int size,
            InternshipSort sort) {
        Page<Internship> internships = internshipRepository.findPostedByUser(
                userId,
                status,
                internshipType,
                workMode,
                paid,
                toSearchPattern(search),
                PageRequest.of(page, size, sort.toSort()));
        return toPage(internships, userId);
    }

    @Transactional
    public InternshipDetailResponse updateInternship(
            UUID userId,
            UUID internshipId,
            UpdateInternshipRequest request) {
        Internship internship = requireInternshipForUpdate(internshipId);
        requireOwner(internship, userId);
        if (request.deadline() != null) {
            validateFutureDeadline(request.deadline());
        }
        internship.update(
                request.title(),
                request.companyName(),
                request.description(),
                request.location(),
                request.internshipType(),
                request.workMode(),
                request.paid(),
                request.stipendAmount(),
                request.currency(),
                request.applyUrl(),
                request.deadline(),
                request.status());
        return toDetail(internship, userId);
    }

    @Transactional
    public void deleteInternship(UUID userId, UUID internshipId) {
        Internship internship = requireInternshipForUpdate(internshipId);
        requireOwner(internship, userId);
        internship.softDelete();
    }

    @Transactional
    public SavedInternshipResponse saveInternship(
            UUID userId,
            UUID internshipId) {
        Internship internship = requireInternshipForUpdate(internshipId);
        SavedInternshipId savedId =
                new SavedInternshipId(internshipId, userId);
        if (savedRepository.existsById(savedId)) {
            throw conflict(
                    "INTERNSHIP_ALREADY_SAVED",
                    "The current user has already saved this internship.");
        }
        User user = requireUser(userId);
        SavedInternship saved = savedRepository.save(
                new SavedInternship(internship, user));
        return internshipMapper.toSaved(saved);
    }

    @Transactional
    public void unsaveInternship(UUID userId, UUID internshipId) {
        requireInternshipForUpdate(internshipId);
        SavedInternshipId savedId =
                new SavedInternshipId(internshipId, userId);
        SavedInternship saved = savedRepository.findById(savedId)
                .orElseThrow(() -> conflict(
                        "INTERNSHIP_NOT_SAVED",
                        "The current user has not saved this internship."));
        savedRepository.delete(saved);
    }

    @Transactional(readOnly = true)
    public InternshipPageResponse listSavedInternships(
            UUID userId,
            int page,
            int size,
            InternshipSort sort) {
        Page<Internship> internships =
                internshipRepository.findSavedByUser(
                        userId,
                        PageRequest.of(page, size, sort.toSort()));
        return toPage(internships, userId);
    }

    @Transactional(readOnly = true)
    public SavedInternshipResponse getSavedState(
            UUID userId,
            UUID internshipId) {
        requireInternship(internshipId);
        SavedInternshipId savedId =
                new SavedInternshipId(internshipId, userId);
        return savedRepository.findById(savedId)
                .map(internshipMapper::toSaved)
                .orElseGet(() -> new SavedInternshipResponse(
                        internshipId,
                        userId,
                        false,
                        null));
    }

    private InternshipPageResponse toPage(
            Page<Internship> page,
            UUID viewerUserId) {
        Set<UUID> savedIds = savedInternshipIds(
                page.getContent(),
                viewerUserId);
        List<InternshipSummaryResponse> content = page.getContent().stream()
                .map(internship -> internshipMapper.toSummary(
                        internship,
                        savedIds.contains(internship.getId()),
                        viewerUserId != null
                                && internship.isOwnedBy(viewerUserId)))
                .toList();
        return new InternshipPageResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    private InternshipDetailResponse toDetail(
            Internship internship,
            UUID viewerUserId) {
        boolean saved = viewerUserId != null
                && savedRepository.existsById(new SavedInternshipId(
                        internship.getId(),
                        viewerUserId));
        return internshipMapper.toDetail(
                internship,
                saved,
                viewerUserId != null
                        && internship.isOwnedBy(viewerUserId));
    }

    private Set<UUID> savedInternshipIds(
            List<Internship> internships,
            UUID viewerUserId) {
        if (viewerUserId == null || internships.isEmpty()) {
            return Set.of();
        }
        List<UUID> internshipIds = internships.stream()
                .map(Internship::getId)
                .toList();
        return new HashSet<>(savedRepository.findSavedInternshipIds(
                viewerUserId,
                internshipIds));
    }

    private void validateFutureDeadline(Instant deadline) {
        if (!deadline.isAfter(clock.instant())) {
            throw conflict(
                    "INTERNSHIP_DEADLINE_NOT_FUTURE",
                    "The internship deadline must be in the future.");
        }
    }

    private String toSearchPattern(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String escaped = search.trim()
                .toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));
    }

    private Internship requireInternship(UUID internshipId) {
        return internshipRepository.findActiveById(internshipId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Internship"));
    }

    private Internship requireInternshipForUpdate(UUID internshipId) {
        return internshipRepository.findActiveByIdForUpdate(internshipId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Internship"));
    }

    private void requireOwner(Internship internship, UUID userId) {
        if (!internship.isOwnedBy(userId)) {
            throw new AccessDeniedException(
                    "Only the internship poster may modify this internship.");
        }
    }

    private InternshipConflictException conflict(
            String code,
            String message) {
        return new InternshipConflictException(code, message);
    }
}
