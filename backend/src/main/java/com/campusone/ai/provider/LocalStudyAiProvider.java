package com.campusone.ai.provider;

import com.campusone.ai.entity.AiSessionMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class LocalStudyAiProvider implements AiProvider {

    public static final String PROVIDER_NAME =
            "LOCAL_STUDY_ASSISTANT";

    private final ObjectMapper objectMapper;

    public LocalStudyAiProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public AiProviderResponse generateChatResponse(
            AiProviderRequest request) {
        String input = compact(request.input());
        boolean explanation = request.mode() == AiSessionMode.EXPLAIN_CONCEPT
                || input.toLowerCase().contains("explain");
        String response;
        if (explanation) {
            response = """
                    Let us break this down step by step:
                    1. Start with the core idea: %s
                    2. Connect it to a small example you already understand.
                    3. Test your understanding by explaining it in your own words.
                    4. Finish with one practice question and review any weak point.
                    """.formatted(input);
        } else {
            response = """
                    Here is a student-friendly way to approach it:
                    %s

                    Focus on the main idea first, then work through one example, and finally check your understanding with a short practice question.
                    """.formatted(input);
        }
        if (request.context() != null
                && !request.context().isBlank()) {
            response += "\n\nUse this course context while studying: "
                    + compact(request.context());
        }
        if (input.length() < 20) {
            response += "\nHelpful follow-up: Which course or topic should I connect this to?";
        }
        return textResponse(response.trim());
    }

    @Override
    public AiProviderResponse generateSummary(
            AiProviderRequest request) {
        List<String> ideas = ideas(request.input());
        ObjectNode content = objectMapper.createObjectNode();
        content.put(
                "shortSummary",
                String.join(
                        " ",
                        ideas.subList(0, Math.min(2, ideas.size()))));
        ArrayNode keyPoints = content.putArray("keyPoints");
        ideas.stream().limit(5).forEach(keyPoints::add);
        content.put(
                "revisionNotes",
                "Review the key points, explain each one aloud, and create one example for every idea.");
        return generatedResponse(
                content.get("shortSummary").asText(),
                content);
    }

    @Override
    public AiProviderResponse generateFlashcards(
            AiProviderRequest request) {
        List<String> ideas = ideas(request.input());
        ArrayNode flashcards = objectMapper.createArrayNode();
        for (int index = 0; index < request.count(); index++) {
            String idea = ideas.get(index % ideas.size());
            ObjectNode card = flashcards.addObject();
            card.put(
                    "question",
                    "What is the key idea in study point "
                            + (index + 1)
                            + "?");
            card.put("answer", idea);
        }
        return generatedResponse(
                "Generated " + request.count() + " revision flashcards.",
                flashcards);
    }

    @Override
    public AiProviderResponse generateQuiz(
            AiProviderRequest request) {
        List<String> ideas = ideas(request.input());
        ArrayNode quiz = objectMapper.createArrayNode();
        for (int index = 0; index < request.count(); index++) {
            String correct = ideas.get(index % ideas.size());
            ObjectNode question = quiz.addObject();
            question.put(
                    "question",
                    "Which option best describes study point "
                            + (index + 1)
                            + "?");
            ArrayNode options = question.putArray("options");
            options.add(correct);
            options.add("A related term without the central idea");
            options.add("An unrelated implementation detail");
            options.add("None of the provided study points");
            question.put("correctAnswer", correct);
            question.put(
                    "explanation",
                    "The correct answer directly reflects the supplied study material.");
        }
        return generatedResponse(
                "Generated " + request.count() + " practice questions.",
                quiz);
    }

    @Override
    public AiProviderResponse generateStudyPlan(
            AiProviderRequest request) {
        List<String> ideas = ideas(
                request.context() == null
                        ? request.input()
                        : request.input() + ". " + request.context());
        ObjectNode content = objectMapper.createObjectNode();
        content.put("goal", compact(request.input()));
        content.put("days", request.days());
        content.put("dailyMinutes", request.dailyMinutes());
        ArrayNode plan = content.putArray("plan");
        for (int day = 1; day <= request.days(); day++) {
            String topic = ideas.get((day - 1) % ideas.size());
            ObjectNode dayPlan = plan.addObject();
            dayPlan.put("day", day);
            dayPlan.put("topic", topic);
            ArrayNode tasks = dayPlan.putArray("tasks");
            tasks.add("Review the core concept and write concise notes.");
            tasks.add("Complete one focused practice exercise.");
            tasks.add("Spend the final minutes on active recall.");
            dayPlan.put("estimatedMinutes", request.dailyMinutes());
        }
        return generatedResponse(
                "Created a " + request.days() + "-day study plan.",
                content);
    }

    private AiProviderResponse textResponse(String text) {
        return new AiProviderResponse(
                text,
                null,
                PROVIDER_NAME);
    }

    private AiProviderResponse generatedResponse(
            String text,
            ObjectNode content) {
        return new AiProviderResponse(
                text,
                content,
                PROVIDER_NAME);
    }

    private AiProviderResponse generatedResponse(
            String text,
            ArrayNode content) {
        return new AiProviderResponse(
                text,
                content,
                PROVIDER_NAME);
    }

    private List<String> ideas(String input) {
        String normalized = compact(input);
        String[] sentences = normalized.split("(?<=[.!?])\\s+");
        List<String> ideas = new ArrayList<>();
        for (String sentence : sentences) {
            String idea = sentence.trim();
            if (!idea.isEmpty()) {
                ideas.add(idea);
            }
        }
        if (ideas.isEmpty()) {
            ideas.add(normalized);
        }
        return ideas;
    }

    private String compact(String value) {
        if (value == null || value.isBlank()) {
            return "the selected study topic";
        }
        return value.trim().replaceAll("\\s+", " ");
    }
}
