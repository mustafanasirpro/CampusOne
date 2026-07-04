package com.campusone.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusone.ai.entity.AiSessionMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LocalStudyAiProviderTest {

    private LocalStudyAiProvider provider;

    @BeforeEach
    void setUp() {
        provider = new LocalStudyAiProvider(new ObjectMapper());
    }

    @Test
    void generateChatResponse_sameInput_isDeterministicWithoutCredentials() {
        AiProviderRequest request = request(
                "Explain binary trees",
                0,
                0,
                0,
                AiSessionMode.EXPLAIN_CONCEPT);

        var first = provider.generateChatResponse(request);
        var second = provider.generateChatResponse(request);

        assertThat(first).isEqualTo(second);
        assertThat(first.provider())
                .isEqualTo(LocalStudyAiProvider.PROVIDER_NAME);
        assertThat(first.text()).contains("step by step");
    }

    @Test
    void generateSummary_returnsStructuredRevisionContent() {
        var response = provider.generateSummary(request(
                "Binary trees organize values hierarchically. "
                        + "Each node can connect to child nodes.",
                0,
                0,
                0,
                AiSessionMode.SUMMARIZE));

        assertThat(response.generatedContent().has("shortSummary")).isTrue();
        assertThat(response.generatedContent().get("keyPoints").isArray())
                .isTrue();
        assertThat(response.generatedContent().has("revisionNotes")).isTrue();
    }

    @Test
    void generateFlashcards_requestedCount_returnsQuestionAnswerPairs() {
        var response = provider.generateFlashcards(request(
                "Encapsulation protects state. Inheritance reuses behavior.",
                4,
                0,
                0,
                AiSessionMode.FLASHCARDS));

        assertThat(response.generatedContent()).hasSize(4);
        assertThat(response.generatedContent().get(0).has("question"))
                .isTrue();
        assertThat(response.generatedContent().get(0).has("answer"))
                .isTrue();
    }

    @Test
    void generateQuiz_requestedCount_returnsCompleteQuestions() {
        var response = provider.generateQuiz(request(
                "A primary key uniquely identifies a database row.",
                3,
                0,
                0,
                AiSessionMode.QUIZ));

        assertThat(response.generatedContent()).hasSize(3);
        assertThat(response.generatedContent().get(0).get("options"))
                .hasSize(4);
        assertThat(response.generatedContent().get(0)
                .has("correctAnswer")).isTrue();
        assertThat(response.generatedContent().get(0)
                .has("explanation")).isTrue();
    }

    @Test
    void generateStudyPlan_requestedDays_returnsDayWisePlan() {
        var response = provider.generateStudyPlan(new AiProviderRequest(
                "Prepare for database systems",
                "Prepare for database systems",
                "Normalization and SQL queries",
                0,
                3,
                60,
                AiSessionMode.STUDY_PLAN));

        assertThat(response.generatedContent().get("days").asInt())
                .isEqualTo(3);
        assertThat(response.generatedContent().get("dailyMinutes").asInt())
                .isEqualTo(60);
        assertThat(response.generatedContent().get("plan")).hasSize(3);
        assertThat(response.generatedContent().get("plan").get(0)
                .has("tasks")).isTrue();
    }

    private AiProviderRequest request(
            String input,
            int count,
            int days,
            int dailyMinutes,
            AiSessionMode mode) {
        return new AiProviderRequest(
                "Study request",
                input,
                null,
                count,
                days,
                dailyMinutes,
                mode);
    }
}
