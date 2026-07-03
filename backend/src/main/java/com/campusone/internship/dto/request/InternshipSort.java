package com.campusone.internship.dto.request;

import org.springframework.data.domain.Sort;

public enum InternshipSort {
    NEWEST,
    OLDEST,
    DEADLINE_ASC,
    DEADLINE_DESC;

    public Sort toSort() {
        return switch (this) {
            case NEWEST -> Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.asc("id"));
            case OLDEST -> Sort.by(
                    Sort.Order.asc("createdAt"),
                    Sort.Order.asc("id"));
            case DEADLINE_ASC -> Sort.by(
                    Sort.Order.asc("deadline"),
                    Sort.Order.asc("id"));
            case DEADLINE_DESC -> Sort.by(
                    Sort.Order.desc("deadline"),
                    Sort.Order.asc("id"));
        };
    }
}
