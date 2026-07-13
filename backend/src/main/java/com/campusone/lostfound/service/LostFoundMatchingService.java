package com.campusone.lostfound.service;

import com.campusone.lostfound.entity.LostFoundItem;
import com.campusone.lostfound.entity.LostFoundItemType;
import com.campusone.lostfound.entity.LostFoundMatch;
import com.campusone.lostfound.entity.LostFoundMatchStatus;
import com.campusone.lostfound.repository.LostFoundItemRepository;
import com.campusone.lostfound.repository.LostFoundMatchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LostFoundMatchingService {

    private static final int MATCH_THRESHOLD = 60;
    private static final int CANDIDATE_LIMIT = 500;

    private final LostFoundItemRepository itemRepository;
    private final LostFoundMatchRepository matchRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public LostFoundMatchingService(
            LostFoundItemRepository itemRepository,
            LostFoundMatchRepository matchRepository,
            ObjectMapper objectMapper,
            Clock clock) {
        this.itemRepository = itemRepository;
        this.matchRepository = matchRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public void suggestMatchesFor(LostFoundItem item) {
        LostFoundItemType oppositeType = item.getType() == LostFoundItemType.LOST
                ? LostFoundItemType.FOUND
                : LostFoundItemType.LOST;
        LocalDate startDate = item.getItemDate().minusDays(30);
        LocalDate endDate = item.getItemDate().plusDays(30);
        var candidates = itemRepository.findMatchCandidates(
                item.getId(),
                item.getUniversity().getId(),
                oppositeType,
                com.campusone.lostfound.entity.LostFoundItemStatus.PUBLISHED,
                clock.instant(),
                startDate,
                endDate,
                PageRequest.of(0, CANDIDATE_LIMIT));

        for (LostFoundItem candidate : candidates) {
            Score score = score(item, candidate);
            if (score.value() < MATCH_THRESHOLD) {
                continue;
            }
            LostFoundItem lostItem = item.getType() == LostFoundItemType.LOST
                    ? item
                    : candidate;
            LostFoundItem foundItem = item.getType() == LostFoundItemType.FOUND
                    ? item
                    : candidate;
            LostFoundMatch match = matchRepository
                    .findByLostItemIdAndFoundItemId(
                            lostItem.getId(),
                            foundItem.getId())
                    .orElseGet(() -> new LostFoundMatch(
                            lostItem,
                            foundItem,
                            score.value(),
                            score.reasons()));
            if (match.getStatus() != LostFoundMatchStatus.REJECTED) {
                match.refreshSuggestion(score.value(), score.reasons());
                matchRepository.save(match);
            }
        }
    }

    private Score score(LostFoundItem item, LostFoundItem candidate) {
        int score = 0;
        ArrayNode reasons = objectMapper.createArrayNode();

        score += addIf(reasons, "Title similarity", titleScore(
                item.getTitle(),
                candidate.getTitle()));
        score += addIf(reasons, "Same category", item.getCategory() == candidate.getCategory()
                ? 20
                : 0);
        score += addIf(reasons, "Brand match", exactOptionalMatch(
                item.getBrand(),
                candidate.getBrand()) ? 10 : 0);
        score += addIf(reasons, "Color match", exactOptionalMatch(
                item.getColor(),
                candidate.getColor()) ? 5 : 0);
        score += addIf(reasons, "Location similarity", tokenOverlap(
                item.getLocationText(),
                candidate.getLocationText()) ? 15 : 0);
        score += addIf(reasons, "Description overlap", jaccardScore(
                item.getDescription(),
                candidate.getDescription()));
        score += addIf(reasons, "Close item date", dateScore(
                item.getItemDate(),
                candidate.getItemDate()));

        return new Score(Math.min(100, score), reasons);
    }

    private int titleScore(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);
        if (normalizedLeft.equals(normalizedRight)) {
            return 30;
        }
        if (normalizedLeft.contains(normalizedRight)
                || normalizedRight.contains(normalizedLeft)) {
            return 24;
        }
        return jaccard(normalizedLeft, normalizedRight) >= 0.5 ? 18 : 0;
    }

    private int jaccardScore(String left, String right) {
        double overlap = jaccard(normalize(left), normalize(right));
        if (overlap >= 0.5) return 10;
        if (overlap >= 0.25) return 6;
        if (overlap > 0.0) return 3;
        return 0;
    }

    private int dateScore(LocalDate left, LocalDate right) {
        long days = Math.abs(ChronoUnit.DAYS.between(left, right));
        if (days == 0) return 10;
        if (days <= 3) return 8;
        if (days <= 7) return 6;
        if (days <= 14) return 4;
        if (days <= 30) return 2;
        return 0;
    }

    private boolean exactOptionalMatch(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);
        return !normalizedLeft.isBlank() && normalizedLeft.equals(normalizedRight);
    }

    private boolean tokenOverlap(String left, String right) {
        Set<String> leftTokens = tokens(normalize(left));
        Set<String> rightTokens = tokens(normalize(right));
        leftTokens.retainAll(rightTokens);
        return !leftTokens.isEmpty();
    }

    private double jaccard(String left, String right) {
        Set<String> leftTokens = tokens(left);
        Set<String> rightTokens = tokens(right);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0;
        }
        Set<String> intersection = new LinkedHashSet<>(leftTokens);
        intersection.retainAll(rightTokens);
        Set<String> union = new LinkedHashSet<>(leftTokens);
        union.addAll(rightTokens);
        return (double) intersection.size() / union.size();
    }

    private Set<String> tokens(String normalized) {
        if (normalized.isBlank()) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(normalized.split(" "))
                .filter(token -> token.length() >= 2)
                .collect(java.util.stream.Collectors.toCollection(
                        LinkedHashSet::new));
    }

    private int addIf(ArrayNode reasons, String label, int points) {
        if (points > 0) {
            ObjectNode reason = reasons.addObject();
            reason.put("reason", label);
            reason.put("points", points);
        }
        return points;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private record Score(int value, ArrayNode reasons) {
    }
}
