package com.campusone.discussion.mapper;

import com.campusone.discussion.dto.response.AnswerPageResponse;
import com.campusone.discussion.dto.response.AnswerResponse;
import com.campusone.discussion.dto.response.DiscussionAuthorResponse;
import com.campusone.discussion.dto.response.QuestionDetailResponse;
import com.campusone.discussion.dto.response.QuestionSummaryResponse;
import com.campusone.discussion.entity.DiscussionAnswer;
import com.campusone.discussion.entity.DiscussionQuestion;
import com.campusone.user.entity.StudentProfile;
import com.campusone.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class DiscussionMapper {

    private static final int BODY_PREVIEW_LENGTH = 240;

    public QuestionSummaryResponse toQuestionSummary(
            DiscussionQuestion question) {
        return new QuestionSummaryResponse(
                question.getId(),
                question.getTitle(),
                preview(question.getBody()),
                question.getCategory(),
                question.getStatus(),
                toAuthor(question.getAuthor()),
                question.getAnswerCount(),
                question.getVoteScore(),
                question.getViewCount(),
                question.getAcceptedAnswerId(),
                question.getCreatedAt(),
                question.getUpdatedAt());
    }

    public QuestionDetailResponse toQuestionDetail(
            DiscussionQuestion question,
            Integer currentUserVote,
            boolean ownedByCurrentUser,
            AnswerPageResponse answers) {
        return new QuestionDetailResponse(
                question.getId(),
                question.getTitle(),
                question.getBody(),
                question.getCategory(),
                question.getStatus(),
                toAuthor(question.getAuthor()),
                question.getAnswerCount(),
                question.getVoteScore(),
                question.getViewCount(),
                question.getAcceptedAnswerId(),
                currentUserVote,
                ownedByCurrentUser,
                answers,
                question.getCreatedAt(),
                question.getUpdatedAt());
    }

    public AnswerResponse toAnswer(
            DiscussionAnswer answer,
            Integer currentUserVote,
            boolean ownedByCurrentUser) {
        return new AnswerResponse(
                answer.getId(),
                answer.getQuestion().getId(),
                answer.getBody(),
                toAuthor(answer.getAuthor()),
                answer.isAccepted(),
                answer.getVoteScore(),
                currentUserVote,
                ownedByCurrentUser,
                answer.getCreatedAt(),
                answer.getUpdatedAt());
    }

    private DiscussionAuthorResponse toAuthor(User author) {
        StudentProfile profile = author.getStudentProfile();
        return new DiscussionAuthorResponse(
                author.getId(),
                profile == null ? null : profile.getFullName(),
                profile == null ? null : profile.getAvatarUrl(),
                profile == null ? null : profile.getUniversity().getName());
    }

    private String preview(String body) {
        if (body.length() <= BODY_PREVIEW_LENGTH) {
            return body;
        }
        return body.substring(0, BODY_PREVIEW_LENGTH - 1) + "…";
    }
}
