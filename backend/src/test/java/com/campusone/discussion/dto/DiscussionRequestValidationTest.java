package com.campusone.discussion.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusone.discussion.dto.request.CreateAnswerRequest;
import com.campusone.discussion.dto.request.CreateQuestionRequest;
import com.campusone.discussion.dto.request.UpdateQuestionRequest;
import com.campusone.discussion.dto.request.VoteRequest;
import com.campusone.discussion.entity.DiscussionCategory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DiscussionRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory()
                .getValidator();
    }

    @Test
    void createQuestion_validRequest_hasNoViolations() {
        CreateQuestionRequest request = new CreateQuestionRequest(
                "How does dependency injection work?",
                "I understand the basic idea but need a practical explanation.",
                DiscussionCategory.PROGRAMMING);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void createQuestion_shortFields_hasValidationViolations() {
        CreateQuestionRequest request = new CreateQuestionRequest(
                "Why",
                "Too short",
                null);

        Set<ConstraintViolation<CreateQuestionRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .extracting(violation ->
                        violation.getPropertyPath().toString())
                .contains("title", "body", "category");
    }

    @Test
    void answer_shortBody_hasValidationViolation() {
        CreateAnswerRequest request = new CreateAnswerRequest("Short");

        assertThat(validator.validate(request))
                .extracting(violation ->
                        violation.getPropertyPath().toString())
                .contains("body");
    }

    @Test
    void vote_zeroValue_hasValidationViolation() {
        VoteRequest request = new VoteRequest(0);

        assertThat(validator.validate(request))
                .extracting(violation ->
                        violation.getPropertyPath().toString())
                .contains("voteValueSupported");
    }

    @Test
    void updateQuestion_blankTitle_hasValidationViolation() {
        UpdateQuestionRequest request = new UpdateQuestionRequest(
                "   ",
                null,
                null,
                null);

        assertThat(validator.validate(request))
                .extracting(violation ->
                        violation.getPropertyPath().toString())
                .contains("title");
    }
}
