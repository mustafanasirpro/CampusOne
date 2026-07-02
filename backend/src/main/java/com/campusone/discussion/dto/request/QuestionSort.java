package com.campusone.discussion.dto.request;

import org.springframework.data.domain.Sort;

public enum QuestionSort {
    NEWEST,
    OLDEST,
    MOST_VOTED,
    MOST_ANSWERED;

    public Sort toSort() {
        return switch (this) {
            case NEWEST -> Sort.by(
                    Sort.Order.desc("createdAt"),
                    Sort.Order.asc("id"));
            case OLDEST -> Sort.by(
                    Sort.Order.asc("createdAt"),
                    Sort.Order.asc("id"));
            case MOST_VOTED -> Sort.by(
                    Sort.Order.desc("voteScore"),
                    Sort.Order.desc("createdAt"),
                    Sort.Order.asc("id"));
            case MOST_ANSWERED -> Sort.by(
                    Sort.Order.desc("answerCount"),
                    Sort.Order.desc("createdAt"),
                    Sort.Order.asc("id"));
        };
    }
}
