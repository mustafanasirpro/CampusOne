package com.campusone.ai.provider;

public interface AiProvider {

    String providerName();

    AiProviderResponse generateChatResponse(AiProviderRequest request);

    AiProviderResponse generateSummary(AiProviderRequest request);

    AiProviderResponse generateFlashcards(AiProviderRequest request);

    AiProviderResponse generateQuiz(AiProviderRequest request);

    AiProviderResponse generateStudyPlan(AiProviderRequest request);
}
