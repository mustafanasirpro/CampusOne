package com.campusone.notification.repository;

import com.campusone.notification.entity.Notification;
import com.campusone.notification.entity.NotificationTargetType;
import com.campusone.notification.entity.NotificationType;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository
        extends JpaRepository<Notification, UUID> {

    @Query("""
            select notification
            from Notification notification
            where notification.recipient.id = :recipientUserId
              and notification.deleted = false
              and (
                    :unreadOnly is null
                    or :unreadOnly = false
                    or notification.readAt is null
              )
              and (:type is null or notification.type = :type)
              and (
                    :targetType is null
                    or notification.targetType = :targetType
              )
            """)
    Page<Notification> findForRecipient(
            @Param("recipientUserId") UUID recipientUserId,
            @Param("unreadOnly") Boolean unreadOnly,
            @Param("type") NotificationType type,
            @Param("targetType") NotificationTargetType targetType,
            Pageable pageable);

    @Query("""
            select notification
            from Notification notification
            where notification.id = :notificationId
              and notification.deleted = false
            """)
    Optional<Notification> findActiveById(
            @Param("notificationId") UUID notificationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select notification
            from Notification notification
            where notification.id = :notificationId
              and notification.deleted = false
            """)
    Optional<Notification> findActiveByIdForUpdate(
            @Param("notificationId") UUID notificationId);

    long countByRecipientIdAndReadAtIsNullAndDeletedFalse(
            UUID recipientUserId);

    @Modifying
    @Query("""
            update Notification notification
            set notification.readAt = :readAt,
                notification.updatedAt = :readAt,
                notification.version = notification.version + 1
            where notification.recipient.id = :recipientUserId
              and notification.deleted = false
              and notification.readAt is null
            """)
    int markAllRead(
            @Param("recipientUserId") UUID recipientUserId,
            @Param("readAt") Instant readAt);
}
