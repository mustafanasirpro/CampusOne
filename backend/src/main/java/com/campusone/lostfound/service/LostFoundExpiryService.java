package com.campusone.lostfound.service;

import com.campusone.lostfound.entity.LostFoundItemStatus;
import com.campusone.lostfound.repository.LostFoundItemRepository;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LostFoundExpiryService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(LostFoundExpiryService.class);

    private final LostFoundItemRepository itemRepository;
    private final Clock clock;
    private final int batchSize;

    public LostFoundExpiryService(
            LostFoundItemRepository itemRepository,
            Clock clock,
            @Value("${app.lost-found.expiry-archive-batch-size:100}")
            int batchSize) {
        this.itemRepository = itemRepository;
        this.clock = clock;
        this.batchSize = Math.max(1, Math.min(batchSize, 500));
    }

    @Scheduled(
            fixedDelayString = "${app.lost-found.expiry-archive-delay:1h}",
            initialDelayString = "${app.lost-found.expiry-archive-initial-delay:5m}")
    @Transactional
    public void archiveExpiredPublishedItems() {
        int archived = archiveExpiredBatch();
        if (archived > 0) {
            LOGGER.info("Archived {} expired Lost & Found items", archived);
        }
    }

    @Transactional
    public int archiveExpiredBatch() {
        Instant now = clock.instant();
        var expiredItems = itemRepository.findExpiredPublishedForUpdate(
                LostFoundItemStatus.PUBLISHED,
                now,
                PageRequest.of(0, batchSize));
        expiredItems.forEach(item -> {
            if (item.getStatus() == LostFoundItemStatus.PUBLISHED) {
                item.archive();
            }
        });
        return expiredItems.size();
    }
}
