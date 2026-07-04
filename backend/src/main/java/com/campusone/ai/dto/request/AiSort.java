package com.campusone.ai.dto.request;

import org.springframework.data.domain.Sort;

public enum AiSort {
    NEWEST,
    OLDEST;

    public Sort toSort() {
        return switch (this) {
            case NEWEST -> Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.asc("id"));
            case OLDEST -> Sort.by(
                    Sort.Order.asc("createdAt"),
                    Sort.Order.asc("id"));
        };
    }
}
