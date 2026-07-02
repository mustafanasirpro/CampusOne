package com.campusone.discussion.dto.request;

import com.campusone.discussion.entity.DiscussionQuestionStatus;

public enum QuestionUpdateStatus {
    OPEN,
    CLOSED;

    public DiscussionQuestionStatus toQuestionStatus() {
        return DiscussionQuestionStatus.valueOf(name());
    }
}
