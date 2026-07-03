package com.campusone.event.service;

import com.campusone.common.exception.ResourceNotFoundException;
import com.campusone.event.dto.request.CreateEventRequest;
import com.campusone.event.dto.request.EventSort;
import com.campusone.event.dto.request.UpdateEventRequest;
import com.campusone.event.dto.response.EventDetailResponse;
import com.campusone.event.dto.response.EventPageResponse;
import com.campusone.event.dto.response.EventParticipantResponse;
import com.campusone.event.dto.response.EventSummaryResponse;
import com.campusone.event.entity.CampusEvent;
import com.campusone.event.entity.EventParticipant;
import com.campusone.event.entity.EventParticipantId;
import com.campusone.event.entity.EventStatus;
import com.campusone.event.entity.EventVisibility;
import com.campusone.event.exception.EventConflictException;
import com.campusone.event.mapper.EventMapper;
import com.campusone.event.repository.CampusEventRepository;
import com.campusone.event.repository.EventParticipantRepository;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
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
public class EventService {

    private final CampusEventRepository eventRepository;
    private final EventParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;

    public EventService(
            CampusEventRepository eventRepository,
            EventParticipantRepository participantRepository,
            UserRepository userRepository,
            EventMapper eventMapper) {
        this.eventRepository = eventRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.eventMapper = eventMapper;
    }

    @Transactional
    public EventDetailResponse createEvent(
            UUID userId,
            CreateEventRequest request) {
        User organizer = requireUser(userId);
        CampusEvent event = eventRepository.save(new CampusEvent(
                organizer,
                request.title(),
                request.description(),
                request.location(),
                request.startTime(),
                request.endTime(),
                request.capacity(),
                request.visibility()));
        return eventMapper.toDetail(event, false, true);
    }

    @Transactional(readOnly = true)
    public EventPageResponse listPublicEvents(
            UUID viewerUserId,
            EventStatus status,
            String search,
            int page,
            int size,
            EventSort sort) {
        Page<CampusEvent> events = eventRepository.findPublicEvents(
                EventVisibility.PUBLIC,
                status,
                toSearchPattern(search),
                PageRequest.of(page, size, sort.toSort()));
        return toPage(events, viewerUserId);
    }

    @Transactional(readOnly = true)
    public EventDetailResponse getEvent(
            UUID eventId,
            UUID viewerUserId) {
        CampusEvent event = requireEvent(eventId);
        requireViewable(event, viewerUserId);
        return toDetail(event, viewerUserId);
    }

    @Transactional(readOnly = true)
    public EventPageResponse listMyEvents(
            UUID userId,
            int page,
            int size,
            EventSort sort) {
        Page<CampusEvent> events = eventRepository.findOrganizedByUser(
                userId,
                PageRequest.of(page, size, sort.toSort()));
        return toPage(events, userId);
    }

    @Transactional(readOnly = true)
    public EventPageResponse listJoinedEvents(
            UUID userId,
            int page,
            int size,
            EventSort sort) {
        Page<CampusEvent> events = eventRepository.findJoinedByUser(
                userId,
                PageRequest.of(page, size, sort.toSort()));
        return toPage(events, userId);
    }

    @Transactional
    public EventDetailResponse updateEvent(
            UUID userId,
            UUID eventId,
            UpdateEventRequest request) {
        CampusEvent event = requireEventForUpdate(eventId);
        requireOrganizer(event, userId);
        validateUpdate(event, request);
        event.update(
                request.title(),
                request.description(),
                request.location(),
                request.startTime(),
                request.endTime(),
                request.capacity(),
                request.visibility(),
                request.status());
        return toDetail(event, userId);
    }

    @Transactional
    public void deleteEvent(UUID userId, UUID eventId) {
        CampusEvent event = requireEventForUpdate(eventId);
        requireOrganizer(event, userId);
        event.softDelete();
    }

    @Transactional
    public EventParticipantResponse joinEvent(
            UUID userId,
            UUID eventId) {
        CampusEvent event = requireEventForUpdate(eventId);
        validateJoin(event, userId);
        EventParticipantId participantId =
                new EventParticipantId(eventId, userId);
        if (participantRepository.existsById(participantId)) {
            throw conflict(
                    "EVENT_ALREADY_JOINED",
                    "The current user has already joined this event.");
        }
        if (event.isAtCapacity()) {
            throw conflict(
                    "EVENT_CAPACITY_FULL",
                    "The event has reached its participant capacity.");
        }

        User user = requireUser(userId);
        EventParticipant participant = participantRepository.save(
                new EventParticipant(event, user));
        event.incrementParticipantCount();
        return eventMapper.toParticipant(
                participant,
                event.getParticipantCount());
    }

    @Transactional
    public void leaveEvent(UUID userId, UUID eventId) {
        CampusEvent event = requireEventForUpdate(eventId);
        EventParticipantId participantId =
                new EventParticipantId(eventId, userId);
        EventParticipant participant = participantRepository.findById(
                        participantId)
                .orElseThrow(() -> conflict(
                        "EVENT_NOT_JOINED",
                        "The current user has not joined this event."));
        participantRepository.delete(participant);
        event.decrementParticipantCount();
    }

    @Transactional(readOnly = true)
    public EventParticipantResponse getParticipantState(
            UUID userId,
            UUID eventId) {
        CampusEvent event = requireEvent(eventId);
        requireViewable(event, userId);
        EventParticipantId participantId =
                new EventParticipantId(eventId, userId);
        return participantRepository.findById(participantId)
                .map(participant -> eventMapper.toParticipant(
                        participant,
                        event.getParticipantCount()))
                .orElseGet(() -> new EventParticipantResponse(
                        eventId,
                        userId,
                        false,
                        null,
                        event.getParticipantCount()));
    }

    private EventPageResponse toPage(
            Page<CampusEvent> page,
            UUID viewerUserId) {
        Set<UUID> joinedEventIds = joinedEventIds(
                page.getContent(),
                viewerUserId);
        List<EventSummaryResponse> content = page.getContent().stream()
                .map(event -> eventMapper.toSummary(
                        event,
                        joinedEventIds.contains(event.getId()),
                        viewerUserId != null
                                && event.isOwnedBy(viewerUserId)))
                .toList();
        return new EventPageResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    private EventDetailResponse toDetail(
            CampusEvent event,
            UUID viewerUserId) {
        boolean joined = viewerUserId != null
                && participantRepository.existsById(
                        new EventParticipantId(
                                event.getId(),
                                viewerUserId));
        return eventMapper.toDetail(
                event,
                joined,
                viewerUserId != null && event.isOwnedBy(viewerUserId));
    }

    private Set<UUID> joinedEventIds(
            List<CampusEvent> events,
            UUID viewerUserId) {
        if (viewerUserId == null || events.isEmpty()) {
            return Set.of();
        }
        List<UUID> eventIds = events.stream()
                .map(CampusEvent::getId)
                .toList();
        return new HashSet<>(participantRepository.findJoinedEventIds(
                viewerUserId,
                eventIds));
    }

    private void validateUpdate(
            CampusEvent event,
            UpdateEventRequest request) {
        Instant resultingStart = request.startTime() == null
                ? event.getStartTime()
                : request.startTime();
        Instant resultingEnd = request.endTime() == null
                ? event.getEndTime()
                : request.endTime();
        if (!resultingEnd.isAfter(resultingStart)) {
            throw conflict(
                    "EVENT_INVALID_TIME_RANGE",
                    "The event end time must be after its start time.");
        }
        if (request.capacity() != null
                && request.capacity() < event.getParticipantCount()) {
            throw conflict(
                    "EVENT_CAPACITY_BELOW_PARTICIPANTS",
                    "Capacity cannot be lower than the current participant count.");
        }
    }

    private void validateJoin(CampusEvent event, UUID userId) {
        if (event.getVisibility() != EventVisibility.PUBLIC
                || event.getStatus() != EventStatus.UPCOMING) {
            throw conflict(
                    "EVENT_NOT_JOINABLE",
                    "Only public upcoming events can be joined.");
        }
        if (event.isOwnedBy(userId)) {
            throw conflict(
                    "EVENT_ORGANIZER_CANNOT_JOIN",
                    "The event organizer cannot join their own event.");
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

    private CampusEvent requireEvent(UUID eventId) {
        return eventRepository.findActiveById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event"));
    }

    private CampusEvent requireEventForUpdate(UUID eventId) {
        return eventRepository.findActiveByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event"));
    }

    private void requireViewable(
            CampusEvent event,
            UUID viewerUserId) {
        if (event.getVisibility() == EventVisibility.PRIVATE
                && (viewerUserId == null
                        || !event.isOwnedBy(viewerUserId))) {
            throw new ResourceNotFoundException("Event");
        }
    }

    private void requireOrganizer(CampusEvent event, UUID userId) {
        if (!event.isOwnedBy(userId)) {
            throw new AccessDeniedException(
                    "Only the event organizer may modify this event.");
        }
    }

    private EventConflictException conflict(
            String code,
            String message) {
        return new EventConflictException(code, message);
    }
}
