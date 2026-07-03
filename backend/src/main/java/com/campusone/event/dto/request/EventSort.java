package com.campusone.event.dto.request;

import org.springframework.data.domain.Sort;

public enum EventSort {
    NEWEST,
    OLDEST,
    UPCOMING;

    public Sort toSort() {
        return switch (this) {
            case NEWEST -> Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.asc("id"));
            case OLDEST -> Sort.by(
                    Sort.Order.asc("createdAt"),
                    Sort.Order.asc("id"));
            case UPCOMING -> Sort.by(
                    Sort.Order.asc("startTime"),
                    Sort.Order.asc("id"));
        };
    }
}
