package com.campusone.ai.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusone.ai.dto.request.CreateAiSessionRequest;
import com.campusone.ai.dto.request.ExplainConceptRequest;
import com.campusone.ai.dto.request.GenerateFlashcardsRequest;
import com.campusone.ai.dto.request.GenerateQuizRequest;
import com.campusone.ai.dto.request.GenerateStudyPlanRequest;
import com.campusone.ai.dto.request.GenerateSummaryRequest;
import com.campusone.ai.dto.request.SendAiMessageRequest;
import com.campusone.ai.entity.AiSessionMode;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AiRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory()
                .getValidator();
    }

    @Test
    void validRequests_haveNoViolationsAndUseDefaults() {
        String studyText = "Binary trees store values in a hierarchical structure.";
        var flashcards = new GenerateFlashcardsRequest(
                null,
                studyText,
                null);
        var quiz = new GenerateQuizRequest(null, studyText, null);

        assertThat(validator.validate(new CreateAiSessionRequest(
                "Database revision",
                AiSessionMode.GENERAL_CHAT))).isEmpty();
        assertThat(validator.validate(new SendAiMessageRequest(
                "Explain normalization"))).isEmpty();
        assertThat(validator.validate(new ExplainConceptRequest(
                "Polymorphism",
                null))).isEmpty();
        assertThat(validator.validate(new GenerateSummaryRequest(
                null,
                studyText))).isEmpty();
        assertThat(validator.validate(flashcards)).isEmpty();
        assertThat(validator.validate(quiz)).isEmpty();
        assertThat(validator.validate(new GenerateStudyPlanRequest(
                "Prepare for final exams",
                7,
                90,
                null))).isEmpty();
        assertThat(flashcards.count()).isEqualTo(5);
        assertThat(quiz.count()).isEqualTo(5);
    }

    @Test
    void invalidSessionAndMessage_areRejected() {
        assertThat(validator.validate(new CreateAiSessionRequest(
                "x",
                null))).hasSize(2);
        assertThat(validator.validate(
                new SendAiMessageRequest(" "))).isNotEmpty();
        assertThat(validator.validate(new SendAiMessageRequest(
                "x".repeat(5001)))).isNotEmpty();
    }

    @Test
    void invalidGenerationBounds_areRejected() {
        String tooShort = "short";

        assertThat(validator.validate(
                new GenerateSummaryRequest("x", tooShort))).hasSize(2);
        assertThat(validator.validate(
                new GenerateFlashcardsRequest(null, tooShort, 21)))
                .hasSize(2);
        assertThat(validator.validate(
                new GenerateQuizRequest(null, tooShort, 0)))
                .hasSize(2);
    }

    @Test
    void invalidStudyPlanBounds_areRejected() {
        var request = new GenerateStudyPlanRequest(
                "plan",
                91,
                9,
                "x".repeat(5001));

        assertThat(validator.validate(request)).hasSize(4);
    }
}
