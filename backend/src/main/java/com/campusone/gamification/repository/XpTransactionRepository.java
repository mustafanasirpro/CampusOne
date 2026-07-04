package com.campusone.gamification.repository;

import com.campusone.gamification.entity.GamificationActionType;
import com.campusone.gamification.entity.GamificationSourceType;
import com.campusone.gamification.entity.XpTransaction;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface XpTransactionRepository
        extends JpaRepository<XpTransaction, UUID> {

    Page<XpTransaction> findByUserId(
            UUID userId,
            Pageable pageable);

    boolean existsByUserIdAndActionTypeAndSourceTypeAndSourceId(
            UUID userId,
            GamificationActionType actionType,
            GamificationSourceType sourceType,
            UUID sourceId);
}
