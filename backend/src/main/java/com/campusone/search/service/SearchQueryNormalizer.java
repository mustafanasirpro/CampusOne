package com.campusone.search.service;

import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class SearchQueryNormalizer {

    public String normalize(String query) {
        return display(query).toLowerCase(Locale.ROOT);
    }

    public String display(String query) {
        if (query == null) {
            return "";
        }
        return query.trim()
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public String compact(String normalizedQuery) {
        return normalizedQuery == null ? "" : normalizedQuery.replace(" ", "");
    }

    public String likePattern(String normalizedQuery) {
        return "%" + escapeLike(normalizedQuery) + "%";
    }

    public String prefixPattern(String normalizedQuery) {
        return escapeLike(normalizedQuery) + "%";
    }

    public String wholeWordPattern(String normalizedQuery) {
        return "% " + escapeLike(normalizedQuery) + " %";
    }

    public String escapeLike(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
