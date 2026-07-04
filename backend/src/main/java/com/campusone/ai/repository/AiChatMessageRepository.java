package com.campusone.ai.repository;

import com.campusone.ai.entity.AiChatMessage;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiChatMessageRepository
        extends JpaRepository<AiChatMessage, UUID> {

    Page<AiChatMessage> findBySessionId(
            UUID sessionId,
            Pageable pageable);
}
