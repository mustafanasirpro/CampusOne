package com.campusone.ai.service;

import com.campusone.ai.dto.request.AiSort;
import com.campusone.ai.dto.request.CreateAiSessionRequest;
import com.campusone.ai.dto.request.ExplainConceptRequest;
import com.campusone.ai.dto.request.GenerateFlashcardsRequest;
import com.campusone.ai.dto.request.GenerateQuizRequest;
import com.campusone.ai.dto.request.GenerateStudyPlanRequest;
import com.campusone.ai.dto.request.GenerateSummaryRequest;
import com.campusone.ai.dto.request.SendAiMessageRequest;
import com.campusone.ai.dto.response.AiChatResponse;
import com.campusone.ai.dto.response.AiExplanationResponse;
import com.campusone.ai.dto.response.AiGeneratedItemPageResponse;
import com.campusone.ai.dto.response.AiGeneratedItemResponse;
import com.campusone.ai.dto.response.AiMessagePageResponse;
import com.campusone.ai.dto.response.AiMessageResponse;
import com.campusone.ai.dto.response.AiSessionDetailResponse;
import com.campusone.ai.dto.response.AiSessionPageResponse;
import com.campusone.ai.dto.response.AiSessionSummaryResponse;
import com.campusone.ai.dto.response.AiUsagePageResponse;
import com.campusone.ai.entity.AiChatMessage;
import com.campusone.ai.entity.AiChatSession;
import com.campusone.ai.entity.AiGeneratedItem;
import com.campusone.ai.entity.AiGeneratedItemType;
import com.campusone.ai.entity.AiMessageRole;
import com.campusone.ai.entity.AiSessionMode;
import com.campusone.ai.entity.AiUsageFeature;
import com.campusone.ai.entity.AiUsageRecord;
import com.campusone.ai.exception.AiProviderException;
import com.campusone.ai.mapper.AiAssistantMapper;
import com.campusone.ai.provider.AiProvider;
import com.campusone.ai.provider.AiProviderRequest;
import com.campusone.ai.provider.AiProviderResponse;
import com.campusone.ai.repository.AiChatMessageRepository;
import com.campusone.ai.repository.AiChatSessionRepository;
import com.campusone.ai.repository.AiGeneratedItemRepository;
import com.campusone.ai.repository.AiUsageRecordRepository;
import com.campusone.common.exception.ResourceNotFoundException;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiAssistantService {

    private static final int SESSION_MESSAGE_PREVIEW_LIMIT = 50;

    private final AiChatSessionRepository sessionRepository;
    private final AiChatMessageRepository messageRepository;
    private final AiGeneratedItemRepository generatedItemRepository;
    private final AiUsageRecordRepository usageRepository;
    private final UserRepository userRepository;
    private final AiProvider aiProvider;
    private final AiAssistantMapper mapper;
    private final Clock clock;

    public AiAssistantService(
            AiChatSessionRepository sessionRepository,
            AiChatMessageRepository messageRepository,
            AiGeneratedItemRepository generatedItemRepository,
            AiUsageRecordRepository usageRepository,
            UserRepository userRepository,
            AiProvider aiProvider,
            AiAssistantMapper mapper,
            Clock clock) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.generatedItemRepository = generatedItemRepository;
        this.usageRepository = usageRepository;
        this.userRepository = userRepository;
        this.aiProvider = aiProvider;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public AiSessionSummaryResponse createSession(
            UUID userId,
            CreateAiSessionRequest request) {
        User user = requireUser(userId);
        AiChatSession session = sessionRepository.save(
                new AiChatSession(
                        user,
                        request.title(),
                        request.mode()));
        return mapper.toSessionSummary(session);
    }

    @Transactional(readOnly = true)
    public AiSessionPageResponse listSessions(
            UUID userId,
            AiSessionMode mode,
            int page,
            int size,
            AiSort sort) {
        Page<AiChatSession> sessions =
                sessionRepository.findOwnedSessions(
                        userId,
                        mode,
                        PageRequest.of(page, size, sort.toSort()));
        List<AiSessionSummaryResponse> content =
                sessions.getContent().stream()
                        .map(mapper::toSessionSummary)
                        .toList();
        return new AiSessionPageResponse(
                content,
                sessions.getNumber(),
                sessions.getSize(),
                sessions.getTotalElements(),
                sessions.getTotalPages(),
                sessions.isFirst(),
                sessions.isLast());
    }

    @Transactional(readOnly = true)
    public AiSessionDetailResponse getSession(
            UUID userId,
            UUID sessionId) {
        AiChatSession session = requireOwnedSession(
                userId,
                sessionId,
                false);
        Page<AiChatMessage> latestMessages =
                messageRepository.findBySessionId(
                        sessionId,
                        PageRequest.of(
                                0,
                                SESSION_MESSAGE_PREVIEW_LIMIT,
                                Sort.by(
                                        Sort.Order.desc("createdAt"),
                                        Sort.Order.desc("id"))));
        List<AiChatMessage> chronological =
                new ArrayList<>(latestMessages.getContent());
        Collections.reverse(chronological);
        return mapper.toSessionDetail(session, chronological);
    }

    @Transactional
    public void deleteSession(
            UUID userId,
            UUID sessionId) {
        requireOwnedSession(userId, sessionId, true)
                .softDelete();
    }

    @Transactional
    public AiChatResponse sendMessage(
            UUID userId,
            UUID sessionId,
            SendAiMessageRequest request) {
        AiChatSession session = requireOwnedSession(
                userId,
                sessionId,
                true);
        User user = session.getUser();
        AiChatMessage userMessage = messageRepository.save(
                new AiChatMessage(
                        session,
                        user,
                        AiMessageRole.USER,
                        request.content(),
                        estimateTokens(request.content())));

        AiProviderResponse providerResponse = providerCall(
                provider -> provider.generateChatResponse(
                        new AiProviderRequest(
                                session.getTitle(),
                                request.content(),
                                null,
                                0,
                                0,
                                0,
                                session.getMode())));
        String assistantContent =
                requireProviderText(providerResponse);
        AiChatMessage assistantMessage = messageRepository.save(
                new AiChatMessage(
                        session,
                        user,
                        AiMessageRole.ASSISTANT,
                        assistantContent,
                        estimateTokens(assistantContent)));
        session.touch(clock.instant());
        recordUsage(
                user,
                featureForMode(session.getMode()),
                request.content(),
                assistantContent,
                providerResponse.provider());
        return new AiChatResponse(
                sessionId,
                mapper.toMessage(userMessage),
                mapper.toMessage(assistantMessage),
                providerResponse.provider());
    }

    @Transactional(readOnly = true)
    public AiMessagePageResponse listMessages(
            UUID userId,
            UUID sessionId,
            int page,
            int size) {
        requireOwnedSession(userId, sessionId, false);
        Page<AiChatMessage> messages =
                messageRepository.findBySessionId(
                        sessionId,
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(
                                        Sort.Order.asc("createdAt"),
                                        Sort.Order.asc("id"))));
        return new AiMessagePageResponse(
                messages.getContent().stream()
                        .map(mapper::toMessage)
                        .toList(),
                messages.getNumber(),
                messages.getSize(),
                messages.getTotalElements(),
                messages.getTotalPages(),
                messages.isFirst(),
                messages.isLast());
    }

    @Transactional
    public AiExplanationResponse explainConcept(
            UUID userId,
            ExplainConceptRequest request) {
        User user = requireUser(userId);
        AiProviderResponse response = providerCall(
                provider -> provider.generateChatResponse(
                        new AiProviderRequest(
                                request.concept(),
                                request.concept(),
                                request.context(),
                                0,
                                0,
                                0,
                                AiSessionMode.EXPLAIN_CONCEPT)));
        String explanation = requireProviderText(response);
        recordUsage(
                user,
                AiUsageFeature.EXPLAIN_CONCEPT,
                combine(request.concept(), request.context()),
                explanation,
                response.provider());
        return new AiExplanationResponse(
                request.concept(),
                explanation,
                response.provider());
    }

    @Transactional
    public AiGeneratedItemResponse generateSummary(
            UUID userId,
            GenerateSummaryRequest request) {
        User user = requireUser(userId);
        AiProviderResponse response = providerCall(
                provider -> provider.generateSummary(
                        providerRequest(
                                request.title(),
                                request.text(),
                                0)));
        return saveGeneratedItem(
                user,
                AiGeneratedItemType.SUMMARY,
                defaultTitle(request.title(), "Study Summary"),
                request.text(),
                AiUsageFeature.SUMMARIZE,
                response);
    }

    @Transactional
    public AiGeneratedItemResponse generateFlashcards(
            UUID userId,
            GenerateFlashcardsRequest request) {
        User user = requireUser(userId);
        AiProviderResponse response = providerCall(
                provider -> provider.generateFlashcards(
                        providerRequest(
                                request.title(),
                                request.text(),
                                request.count())));
        return saveGeneratedItem(
                user,
                AiGeneratedItemType.FLASHCARDS,
                defaultTitle(request.title(), "Study Flashcards"),
                request.text(),
                AiUsageFeature.FLASHCARDS,
                response);
    }

    @Transactional
    public AiGeneratedItemResponse generateQuiz(
            UUID userId,
            GenerateQuizRequest request) {
        User user = requireUser(userId);
        AiProviderResponse response = providerCall(
                provider -> provider.generateQuiz(
                        providerRequest(
                                request.title(),
                                request.text(),
                                request.count())));
        return saveGeneratedItem(
                user,
                AiGeneratedItemType.QUIZ,
                defaultTitle(request.title(), "Practice Quiz"),
                request.text(),
                AiUsageFeature.QUIZ,
                response);
    }

    @Transactional
    public AiGeneratedItemResponse generateStudyPlan(
            UUID userId,
            GenerateStudyPlanRequest request) {
        User user = requireUser(userId);
        AiProviderResponse response = providerCall(
                provider -> provider.generateStudyPlan(
                        new AiProviderRequest(
                                request.goal(),
                                request.goal(),
                                request.context(),
                                0,
                                request.days(),
                                request.dailyMinutes(),
                                AiSessionMode.STUDY_PLAN)));
        return saveGeneratedItem(
                user,
                AiGeneratedItemType.STUDY_PLAN,
                studyPlanTitle(request.goal()),
                combine(request.goal(), request.context()),
                AiUsageFeature.STUDY_PLAN,
                response);
    }

    @Transactional(readOnly = true)
    public AiGeneratedItemPageResponse listGeneratedItems(
            UUID userId,
            AiGeneratedItemType itemType,
            int page,
            int size) {
        Page<AiGeneratedItem> items =
                generatedItemRepository.findOwnedItems(
                        userId,
                        itemType,
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(
                                        Sort.Order.desc("createdAt"),
                                        Sort.Order.asc("id"))));
        return new AiGeneratedItemPageResponse(
                items.getContent().stream()
                        .map(mapper::toGeneratedItem)
                        .toList(),
                items.getNumber(),
                items.getSize(),
                items.getTotalElements(),
                items.getTotalPages(),
                items.isFirst(),
                items.isLast());
    }

    @Transactional(readOnly = true)
    public AiGeneratedItemResponse getGeneratedItem(
            UUID userId,
            UUID itemId) {
        return mapper.toGeneratedItem(
                requireOwnedGeneratedItem(userId, itemId, false));
    }

    @Transactional
    public void deleteGeneratedItem(
            UUID userId,
            UUID itemId) {
        requireOwnedGeneratedItem(userId, itemId, true)
                .softDelete();
    }

    @Transactional(readOnly = true)
    public AiUsagePageResponse listUsage(
            UUID userId,
            AiUsageFeature feature,
            int page,
            int size) {
        Page<AiUsageRecord> usage = usageRepository.findOwnedUsage(
                userId,
                feature,
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Order.desc("createdAt"),
                                Sort.Order.asc("id"))));
        return new AiUsagePageResponse(
                usage.getContent().stream()
                        .map(mapper::toUsage)
                        .toList(),
                usage.getNumber(),
                usage.getSize(),
                usage.getTotalElements(),
                usage.getTotalPages(),
                usage.isFirst(),
                usage.isLast());
    }

    public int estimateTokens(String content) {
        if (content == null || content.isEmpty()) {
            return 1;
        }
        return Math.max(1, content.length() / 4);
    }

    private AiGeneratedItemResponse saveGeneratedItem(
            User user,
            AiGeneratedItemType itemType,
            String title,
            String sourceText,
            AiUsageFeature feature,
            AiProviderResponse response) {
        JsonNode generatedContent =
                requireGeneratedContent(response);
        AiGeneratedItem item = generatedItemRepository.save(
                new AiGeneratedItem(
                        user,
                        null,
                        itemType,
                        title,
                        sourceText,
                        generatedContent));
        recordUsage(
                user,
                feature,
                sourceText,
                generatedContent.toString(),
                response.provider());
        return mapper.toGeneratedItem(item);
    }

    private void recordUsage(
            User user,
            AiUsageFeature feature,
            String input,
            String output,
            String provider) {
        usageRepository.save(new AiUsageRecord(
                user,
                feature,
                estimateTokens(input),
                estimateTokens(output),
                provider));
    }

    private AiChatSession requireOwnedSession(
            UUID userId,
            UUID sessionId,
            boolean forUpdate) {
        AiChatSession session = (forUpdate
                ? sessionRepository.findActiveByIdForUpdate(sessionId)
                : sessionRepository.findActiveById(sessionId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "AI chat session"));
        if (!session.isOwnedBy(userId)) {
            throw new AccessDeniedException(
                    "Only the session owner may access this AI chat.");
        }
        return session;
    }

    private AiGeneratedItem requireOwnedGeneratedItem(
            UUID userId,
            UUID itemId,
            boolean forUpdate) {
        AiGeneratedItem item = (forUpdate
                ? generatedItemRepository.findActiveByIdForUpdate(itemId)
                : generatedItemRepository.findActiveById(itemId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "AI generated item"));
        if (!item.isOwnedBy(userId)) {
            throw new AccessDeniedException(
                    "Only the item owner may access this generated item.");
        }
        return item;
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));
    }

    private AiProviderResponse providerCall(
            Function<AiProvider, AiProviderResponse> operation) {
        try {
            AiProviderResponse response = operation.apply(aiProvider);
            if (response == null
                    || response.provider() == null
                    || response.provider().isBlank()) {
                throw new AiProviderException(
                        "The AI provider returned an invalid response.");
            }
            return response;
        } catch (AiProviderException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AiProviderException(
                    "The study assistant could not generate a response.",
                    exception);
        }
    }

    private String requireProviderText(AiProviderResponse response) {
        if (response.text() == null
                || response.text().isBlank()
                || response.text().length() > 10000) {
            throw new AiProviderException(
                    "The AI provider returned invalid text content.");
        }
        return response.text().trim();
    }

    private JsonNode requireGeneratedContent(
            AiProviderResponse response) {
        if (response.generatedContent() == null
                || response.generatedContent().isNull()) {
            throw new AiProviderException(
                    "The AI provider returned no generated content.");
        }
        return response.generatedContent();
    }

    private AiProviderRequest providerRequest(
            String title,
            String input,
            int count) {
        return new AiProviderRequest(
                title,
                input,
                null,
                count,
                0,
                0,
                AiSessionMode.GENERAL_CHAT);
    }

    private AiUsageFeature featureForMode(AiSessionMode mode) {
        return switch (mode) {
            case GENERAL_CHAT -> AiUsageFeature.CHAT;
            case EXPLAIN_CONCEPT -> AiUsageFeature.EXPLAIN_CONCEPT;
            case SUMMARIZE -> AiUsageFeature.SUMMARIZE;
            case FLASHCARDS -> AiUsageFeature.FLASHCARDS;
            case QUIZ -> AiUsageFeature.QUIZ;
            case STUDY_PLAN -> AiUsageFeature.STUDY_PLAN;
        };
    }

    private String defaultTitle(
            String title,
            String fallback) {
        return title == null ? fallback : title;
    }

    private String studyPlanTitle(String goal) {
        String title = "Study Plan: " + goal;
        return title.length() <= 160
                ? title
                : title.substring(0, 160);
    }

    private String combine(
            String first,
            String second) {
        return second == null || second.isBlank()
                ? first
                : first + "\n\n" + second;
    }
}
