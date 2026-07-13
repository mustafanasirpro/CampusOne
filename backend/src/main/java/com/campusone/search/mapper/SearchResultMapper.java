package com.campusone.search.mapper;

import com.campusone.search.dto.SearchType;
import com.campusone.search.dto.response.SearchResultResponse;
import com.campusone.search.repository.SearchDocument;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SearchResultMapper {

    private static final int SNIPPET_LENGTH = 180;
    private static final int SNIPPET_CONTEXT = 60;

    public SearchResultResponse toResponse(
            SearchDocument document,
            String normalizedQuery) {
        return new SearchResultResponse(
                document.id(),
                document.type(),
                document.title(),
                snippet(document.snippetSource(), normalizedQuery),
                targetUrl(document.type(), document.id().toString()),
                document.ownerOrAuthorName(),
                metadata(document),
                document.createdAt(),
                document.updatedAt());
    }

    private Map<String, Object> metadata(SearchDocument document) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        put(metadata, "category", document.category());
        put(metadata, "location", document.location());
        put(metadata, "companyName", document.companyName());
        put(metadata, "price", document.price());
        put(metadata, "currency", document.currency());
        put(metadata, "status", document.status());
        put(metadata, "date", document.relevantDate());
        return Collections.unmodifiableMap(metadata);
    }

    private String targetUrl(SearchType type, String id) {
        return switch (type) {
            case NOTE -> "/notes/" + id;
            case MARKETPLACE -> "/marketplace/" + id;
            case DISCUSSION -> "/discussions/questions/" + id;
            case EVENT -> "/events/" + id;
            case INTERNSHIP -> "/internships/" + id;
            case LOST_FOUND -> "/lost-found/" + id;
        };
    }

    private String snippet(
            String source,
            String normalizedQuery) {
        if (source == null || source.isBlank()) {
            return "";
        }
        String compact = source.trim().replaceAll("\\s+", " ");
        if (compact.length() <= SNIPPET_LENGTH) {
            return compact;
        }

        String lowerCompact = compact.toLowerCase(Locale.ROOT);
        int matchIndex = lowerCompact.indexOf(normalizedQuery);
        if (matchIndex < 0) {
            matchIndex = firstTokenMatch(lowerCompact, normalizedQuery);
        }
        int start = matchIndex < 0
                ? 0
                : Math.max(0, matchIndex - SNIPPET_CONTEXT);
        int end = Math.min(
                compact.length(),
                start + SNIPPET_LENGTH);
        if (end == compact.length()) {
            start = Math.max(0, end - SNIPPET_LENGTH);
        }
        String result = compact.substring(start, end);
        return (start > 0 ? "…" : "")
                + result
                + (end < compact.length() ? "…" : "");
    }

    private int firstTokenMatch(
            String lowerCompact,
            String normalizedQuery) {
        int firstIndex = -1;
        for (String token : normalizedQuery.split("\\s+")) {
            if (token.isBlank()) {
                continue;
            }
            int tokenIndex = lowerCompact.indexOf(token);
            if (tokenIndex >= 0
                    && (firstIndex < 0 || tokenIndex < firstIndex)) {
                firstIndex = tokenIndex;
            }
        }
        return firstIndex;
    }

    private void put(
            Map<String, Object> metadata,
            String key,
            Object value) {
        if (value != null) {
            metadata.put(key, value);
        }
    }
}
