package com.campusone.event.service;

import com.campusone.common.service.CommunityIntegrationService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.common.exception.ResourceNotFoundException;
import com.campusone.event.dto.request.CreateEventRequest;
import com.campusone.event.dto.request.EventSort;
import com.campusone.event.dto.request.UpdateEventRequest;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    private static final UUID ORGANIZER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");
    private static final UUID ATTENDEE_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000002");
    private static final UUID EVENT_ID = UUID.fromString(
            "50000000-0000-4000-8000-000000000001");
    private static final Instant START =
            Instant.parse("2026-08-15T09:00:00Z");
    private static final Instant END =
            Instant.parse("2026-08-15T13:00:00Z");
    private static final Instant NOW =
            Instant.parse("2026-07-03T12:00:00Z");

    @Mock
    private CampusEventRepository eventRepository;

    @Mock
    private EventParticipantRepository participantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommunityIntegrationService integrationService;

    private EventService eventService;
    private User organizer;
    private User attendee;
    private CampusEvent event;

    @BeforeEach
    void setUp() {
        organizer = user(ORGANIZER_ID, "organizer@example.com");
        attendee = user(ATTENDEE_ID, "attendee@example.com");
        event = event(EventVisibility.PUBLIC, 120);
        eventService = new EventService(
                eventRepository,
                participantRepository,
                userRepository,
                new EventMapper(),
                integrationService);
        lenient().when(participantRepository.findJoinedEventIds(
                        any(UUID.class),
                        any()))
                .thenReturn(List.of());
    }

    @Test
    void createEvent_validRequest_createsUpcomingEvent() {
        when(userRepository.findById(ORGANIZER_ID))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.save(any(CampusEvent.class)))
                .thenAnswer(invocation -> {
                    CampusEvent saved = invocation.getArgument(0);
                    setPersistenceFields(saved);
                    return saved;
                });

        var response = eventService.createEvent(
                ORGANIZER_ID,
                createRequest());

        assertThat(response.id()).isEqualTo(EVENT_ID);
        assertThat(response.status()).isEqualTo(EventStatus.UPCOMING);
        assertThat(response.participantCount()).isZero();
        assertThat(response.ownedByCurrentUser()).isTrue();
    }

    @Test
    void listPublicEvents_returnsPublicPage() {
        when(eventRepository.findPublicEvents(
                eq(EventVisibility.PUBLIC),
                eq(EventStatus.UPCOMING),
                eq("%islamabad%"),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event)));

        var response = eventService.listPublicEvents(
                null,
                EventStatus.UPCOMING,
                " Islamabad ",
                0,
                20,
                EventSort.UPCOMING);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().id()).isEqualTo(EVENT_ID);
    }

    @Test
    void getEvent_publicEvent_isVisibleAnonymously() {
        when(eventRepository.findActiveById(EVENT_ID))
                .thenReturn(Optional.of(event));

        var response = eventService.getEvent(EVENT_ID, null);

        assertThat(response.id()).isEqualTo(EVENT_ID);
        assertThat(response.visibility()).isEqualTo(EventVisibility.PUBLIC);
    }

    @Test
    void getEvent_privateEvent_isVisibleToOrganizer() {
        CampusEvent privateEvent = event(EventVisibility.PRIVATE, 120);
        when(eventRepository.findActiveById(EVENT_ID))
                .thenReturn(Optional.of(privateEvent));

        var response = eventService.getEvent(
                EVENT_ID,
                ORGANIZER_ID);

        assertThat(response.visibility()).isEqualTo(EventVisibility.PRIVATE);
        assertThat(response.ownedByCurrentUser()).isTrue();
    }

    @Test
    void getEvent_privateEvent_isHiddenFromOtherUsers() {
        CampusEvent privateEvent = event(EventVisibility.PRIVATE, 120);
        when(eventRepository.findActiveById(EVENT_ID))
                .thenReturn(Optional.of(privateEvent));

        assertThatThrownBy(() -> eventService.getEvent(
                EVENT_ID,
                ATTENDEE_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listMyEvents_returnsOrganizedEvents() {
        when(eventRepository.findOrganizedByUser(
                eq(ORGANIZER_ID),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event)));

        var response = eventService.listMyEvents(
                ORGANIZER_ID,
                0,
                20,
                EventSort.UPCOMING);

        assertThat(response.content().getFirst().ownedByCurrentUser()).isTrue();
    }

    @Test
    void listJoinedEvents_returnsJoinedEvents() {
        when(eventRepository.findJoinedByUser(
                eq(ATTENDEE_ID),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event)));
        when(participantRepository.findJoinedEventIds(
                ATTENDEE_ID,
                List.of(EVENT_ID)))
                .thenReturn(List.of(EVENT_ID));

        var response = eventService.listJoinedEvents(
                ATTENDEE_ID,
                0,
                20,
                EventSort.UPCOMING);

        assertThat(response.content().getFirst().joinedByCurrentUser()).isTrue();
    }

    @Test
    void updateEvent_organizerCanUpdate() {
        when(eventRepository.findActiveByIdForUpdate(EVENT_ID))
                .thenReturn(Optional.of(event));
        UpdateEventRequest request = new UpdateEventRequest(
                "Advanced Campus AI Workshop",
                null,
                "COMSATS Islamabad, Main Auditorium",
                null,
                null,
                200,
                EventVisibility.PRIVATE,
                EventStatus.UPCOMING);

        var response = eventService.updateEvent(
                ORGANIZER_ID,
                EVENT_ID,
                request);

        assertThat(response.title()).isEqualTo(
                "Advanced Campus AI Workshop");
        assertThat(response.capacity()).isEqualTo(200);
        assertThat(response.visibility()).isEqualTo(EventVisibility.PRIVATE);
    }

    @Test
    void updateEvent_nonOrganizerIsRejected() {
        when(eventRepository.findActiveByIdForUpdate(EVENT_ID))
                .thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.updateEvent(
                ATTENDEE_ID,
                EVENT_ID,
                new UpdateEventRequest(
                        "Unauthorized event update",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateEvent_capacityBelowParticipantsIsRejected() {
        event.incrementParticipantCount();
        when(eventRepository.findActiveByIdForUpdate(EVENT_ID))
                .thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.updateEvent(
                ORGANIZER_ID,
                EVENT_ID,
                new UpdateEventRequest(
                        null,
                        null,
                        null,
                        null,
                        null,
                        0,
                        null,
                        null)))
                .isInstanceOf(EventConflictException.class)
                .hasMessageContaining("participant count");
    }

    @Test
    void deleteEvent_organizerSoftDeletesEvent() {
        when(eventRepository.findActiveByIdForUpdate(EVENT_ID))
                .thenReturn(Optional.of(event));

        eventService.deleteEvent(ORGANIZER_ID, EVENT_ID);

        assertThat(event.isDeleted()).isTrue();
    }

    @Test
    void joinEvent_validParticipant_incrementsCount() {
        when(eventRepository.findActiveByIdForUpdate(EVENT_ID))
                .thenReturn(Optional.of(event));
        when(userRepository.findById(ATTENDEE_ID))
                .thenReturn(Optional.of(attendee));
        when(participantRepository.save(any(EventParticipant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = eventService.joinEvent(ATTENDEE_ID, EVENT_ID);

        assertThat(response.joined()).isTrue();
        assertThat(response.participantCount()).isEqualTo(1);
    }

    @Test
    void joinEvent_duplicateParticipantIsRejected() {
        when(eventRepository.findActiveByIdForUpdate(EVENT_ID))
                .thenReturn(Optional.of(event));
        when(participantRepository.existsById(
                new EventParticipantId(EVENT_ID, ATTENDEE_ID)))
                .thenReturn(true);

        assertThatThrownBy(() -> eventService.joinEvent(
                ATTENDEE_ID,
                EVENT_ID))
                .isInstanceOf(EventConflictException.class)
                .extracting(exception ->
                        ((EventConflictException) exception).getCode())
                .isEqualTo("EVENT_ALREADY_JOINED");
    }

    @Test
    void joinEvent_organizerIsRejected() {
        when(eventRepository.findActiveByIdForUpdate(EVENT_ID))
                .thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.joinEvent(
                ORGANIZER_ID,
                EVENT_ID))
                .isInstanceOf(EventConflictException.class)
                .extracting(exception ->
                        ((EventConflictException) exception).getCode())
                .isEqualTo("EVENT_ORGANIZER_CANNOT_JOIN");
    }

    @Test
    void joinEvent_fullEventIsRejected() {
        CampusEvent fullEvent = event(EventVisibility.PUBLIC, 1);
        fullEvent.incrementParticipantCount();
        when(eventRepository.findActiveByIdForUpdate(EVENT_ID))
                .thenReturn(Optional.of(fullEvent));

        assertThatThrownBy(() -> eventService.joinEvent(
                ATTENDEE_ID,
                EVENT_ID))
                .isInstanceOf(EventConflictException.class)
                .extracting(exception ->
                        ((EventConflictException) exception).getCode())
                .isEqualTo("EVENT_CAPACITY_FULL");
    }

    @Test
    void leaveEvent_joinedParticipant_decrementsCount() {
        event.incrementParticipantCount();
        EventParticipant participant =
                new EventParticipant(event, attendee);
        EventParticipantId participantId =
                new EventParticipantId(EVENT_ID, ATTENDEE_ID);
        when(eventRepository.findActiveByIdForUpdate(EVENT_ID))
                .thenReturn(Optional.of(event));
        when(participantRepository.findById(participantId))
                .thenReturn(Optional.of(participant));

        eventService.leaveEvent(ATTENDEE_ID, EVENT_ID);

        assertThat(event.getParticipantCount()).isZero();
        verify(participantRepository).delete(participant);
    }

    @Test
    void leaveEvent_notJoinedIsRejected() {
        when(eventRepository.findActiveByIdForUpdate(EVENT_ID))
                .thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.leaveEvent(
                ATTENDEE_ID,
                EVENT_ID))
                .isInstanceOf(EventConflictException.class)
                .extracting(exception ->
                        ((EventConflictException) exception).getCode())
                .isEqualTo("EVENT_NOT_JOINED");
    }

    @Test
    void participantState_notJoined_returnsFalse() {
        when(eventRepository.findActiveById(EVENT_ID))
                .thenReturn(Optional.of(event));

        var response = eventService.getParticipantState(
                ATTENDEE_ID,
                EVENT_ID);

        assertThat(response.joined()).isFalse();
        assertThat(response.userId()).isEqualTo(ATTENDEE_ID);
    }

    private CreateEventRequest createRequest() {
        return new CreateEventRequest(
                "Campus AI Workshop",
                "A practical workshop covering responsible AI development.",
                "COMSATS Islamabad, Seminar Hall",
                START,
                END,
                120,
                EventVisibility.PUBLIC);
    }

    private CampusEvent event(
            EventVisibility visibility,
            int capacity) {
        CampusEvent result = new CampusEvent(
                organizer,
                "Campus AI Workshop",
                "A practical workshop covering responsible AI development.",
                "COMSATS Islamabad, Seminar Hall",
                START,
                END,
                capacity,
                visibility);
        setPersistenceFields(result);
        return result;
    }

    private User user(UUID id, String email) {
        User user = new User(email, "$2a$12$encoded-password");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private void setPersistenceFields(CampusEvent target) {
        ReflectionTestUtils.setField(target, "id", EVENT_ID);
        ReflectionTestUtils.setField(target, "createdAt", NOW);
        ReflectionTestUtils.setField(target, "updatedAt", NOW);
    }
}
