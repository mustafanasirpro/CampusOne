package com.campusone.aura.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

import com.campusone.moderation.repository.ModeratorRepository;
import com.campusone.notification.entity.NotificationTargetType;
import com.campusone.notification.entity.NotificationType;
import com.campusone.notification.service.NotificationService;
import com.campusone.user.repository.UserRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuraNotificationServiceTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ModeratorRepository moderatorRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    void notifyTimetablePublished_deduplicatesStudentsAndExcludesActor() {
        UUID actor = UUID.randomUUID();
        UUID student = UUID.randomUUID();
        UUID version = UUID.randomUUID();
        AuraNotificationService service = new AuraNotificationService(
                notificationService,
                moderatorRepository,
                userRepository,
                "");

        service.notifyTimetablePublished(
                List.of(student, actor, student), actor, version);

        verify(notificationService).createBulkNotifications(
                new LinkedHashSet<>(List.of(student)),
                NotificationType.AURA_UPDATE,
                "A new timetable is available",
                "Your latest university timetable is ready to view.",
                NotificationTargetType.AURA_TIMETABLE,
                version,
                "/timetable");
    }

    @Test
    void notifyInstructors_usesInstructorRouteAndDoesNotPropagateFailure() {
        UUID actor = UUID.randomUUID();
        UUID instructor = UUID.randomUUID();
        UUID version = UUID.randomUUID();
        AuraNotificationService service = new AuraNotificationService(
                notificationService,
                moderatorRepository,
                userRepository,
                "");
        LinkedHashSet<UUID> recipients =
                new LinkedHashSet<>(List.of(instructor));
        doThrow(new IllegalStateException("notification storage unavailable"))
                .when(notificationService)
                .createBulkNotifications(
                        recipients,
                        NotificationType.AURA_UPDATE,
                        "Your teaching timetable is available",
                        "The latest published teaching timetable is ready to view.",
                        NotificationTargetType.AURA_TIMETABLE,
                        version,
                        "/instructor-timetable");

        service.notifyInstructorsTimetablePublished(
                List.of(instructor, actor, instructor), actor, version);

        verify(notificationService).createBulkNotifications(
                recipients,
                NotificationType.AURA_UPDATE,
                "Your teaching timetable is available",
                "The latest published teaching timetable is ready to view.",
                NotificationTargetType.AURA_TIMETABLE,
                version,
                "/instructor-timetable");
    }
}
