package com.campusone.ai.repository;

import com.campusone.ai.entity.AiChatSession;
import com.campusone.ai.entity.AiSessionMode;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiChatSessionRepository
        extends JpaRepository<AiChatSession, UUID> {

    @Query("""
            select session
            from AiChatSession session
            where session.user.id = :userId
              and session.deleted = false
              and (:mode is null or session.mode = :mode)
            """)
    Page<AiChatSession> findOwnedSessions(
            @Param("userId") UUID userId,
            @Param("mode") AiSessionMode mode,
            Pageable pageable);

    @Query("""
            select session
            from AiChatSession session
            where session.id = :sessionId
              and session.deleted = false
            """)
    Optional<AiChatSession> findActiveById(
            @Param("sessionId") UUID sessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select session
            from AiChatSession session
            where session.id = :sessionId
              and session.deleted = false
            """)
    Optional<AiChatSession> findActiveByIdForUpdate(
            @Param("sessionId") UUID sessionId);
}
