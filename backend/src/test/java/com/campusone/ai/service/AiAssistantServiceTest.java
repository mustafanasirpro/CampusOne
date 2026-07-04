package com.campusone.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.ai.dto.request.AiSort;
import com.campusone.ai.dto.request.CreateAiSessionRequest;
import com.campusone.ai.dto.request.ExplainConceptRequest;
import com.campusone.ai.dto.request.GenerateFlashcardsRequest;
import com.campusone.ai.dto.request.GenerateQuizRequest;
import com.campusone.ai.dto.request.GenerateStudyPlanRequest;
import com.campusone.ai.dto.request.GenerateSummaryRequest;
import com.campusone.ai.dto.request.SendAiMessageRequest;
import com.campusone.ai.entity.AiChatMessage;
import com.campusone.ai.entity.AiChatSession;
import com.campusone.ai.entity.AiGeneratedItem;
import com.campusone.ai.entity.AiGeneratedItemType;
import com.campusone.ai.entity.AiMessageRole;
import com.campusone.ai.entity.AiSessionMode;
import com.campusone.ai.entity.AiUsageFeature;
import com.campusone.ai.entity.AiUsageRecord;
import com.campusone.ai.mapper.AiAssistantMapper;
import com.campusone.ai.provider.LocalStudyAiProvider;
import com.campusone.ai.repository.AiChatMessageRepository;
import com.campusone.ai.repository.AiChatSessionRepository;
import com.campusone.ai.repository.AiGeneratedItemRepository;
import com.campusone.ai.repository.AiUsageRecordRepository;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AiAssistantServiceTest {

    private static final UUID USER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000002");
    private static final UUID SESSION_ID = UUID.fromString(
            "a1000000-0000-4000-8000-000000000001");
    private static final UUID ITEM_ID = UUID.fromString(
            "a2000000-0000-4000-8000-000000000001");
    private static final UUID MESSAGE_ID = UUID.fromString(
            "a3000000-0000-4000-8000-000000000001");
    private static final UUID USAGE_ID = UUID.fromString(
            "a4000000-0000-4000-8000-000000000001");
    private static final Instant NOW =
            Instant.parse("2026-07-04T08:00:00Z");
    private static final String STUDY_TEXT =
            "Binary trees organize values hierarchically. "
                    + "Each node can reference child nodes.";

    @Mock
    private AiChatSessionRepository sessionRepository;

    @Mock
    private AiChatMessageRepository messageRepository;

    @Mock
    private AiGeneratedItemRepository generatedItemRepository;

    @Mock
    private AiUsageRecordRepository usageRepository;

    @Mock
    private UserRepository userRepository;

    private AiAssistantService service;
    private User user;
    private AiChatSession session;
    private AiGeneratedItem generatedItem;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        user = user(USER_ID);
        session = session(user);
        generatedItem = generatedItem(user, objectMapper);
        service = new AiAssistantService(
                sessionRepository,
                messageRepository,
                generatedItemRepository,
                usageRepository,
                userRepository,
                new LocalStudyAiProvider(objectMapper),
                new AiAssistantMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createSession_validRequest_persistsOwnedSession() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        when(sessionRepository.save(any(AiChatSession.class)))
                .thenAnswer(invocation -> persistSession(
                        invocation.getArgument(0)));

        var response = service.createSession(
                USER_ID,
                new CreateAiSessionRequest(
                        "OOP revision",
                        AiSessionMode.GENERAL_CHAT));

        assertThat(response.id()).isEqualTo(SESSION_ID);
        assertThat(response.title()).isEqualTo("OOP revision");
        assertThat(response.mode())
                .isEqualTo(AiSessionMode.GENERAL_CHAT);
    }

    @Test
    void listSessions_ownedSessions_returnsPage() {
        when(sessionRepository.findOwnedSessions(
                eq(USER_ID),
                eq(AiSessionMode.GENERAL_CHAT),
                any(PageRequest.class)))
                .thenReturn(new PageImpl<>(
                        List.of(session),
                        PageRequest.of(0, 20),
                        1));

        var response = service.listSessions(
                USER_ID,
                AiSessionMode.GENERAL_CHAT,
                0,
                20,
                AiSort.NEWEST);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().id())
                .isEqualTo(SESSION_ID);
    }

    @Test
    void getSession_ownedSession_returnsChronologicalMessages() {
        AiChatMessage assistant = message(
                session,
                AiMessageRole.ASSISTANT,
                "Assistant answer",
                MESSAGE_ID);
        AiChatMessage question = message(
                session,
                AiMessageRole.USER,
                "Student question",
                UUID.randomUUID());
        when(sessionRepository.findActiveById(SESSION_ID))
                .thenReturn(Optional.of(session));
        when(messageRepository.findBySessionId(
                eq(SESSION_ID),
                any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(assistant, question)));

        var response = service.getSession(USER_ID, SESSION_ID);

        assertThat(response.messages()).extracting(message -> message.role())
                .containsExactly(
                        AiMessageRole.USER,
                        AiMessageRole.ASSISTANT);
    }

    @Test
    void getSession_differentOwner_rejectsAccess() {
        when(sessionRepository.findActiveById(SESSION_ID))
                .thenReturn(Optional.of(session));

        assertThatThrownBy(() ->
                service.getSession(OTHER_USER_ID, SESSION_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteSession_ownedSession_softDeletes() {
        when(sessionRepository.findActiveByIdForUpdate(SESSION_ID))
                .thenReturn(Optional.of(session));

        service.deleteSession(USER_ID, SESSION_ID);

        assertThat(session.isDeleted()).isTrue();
    }

    @Test
    void sendMessage_ownedSession_savesPairAndUsage() {
        when(sessionRepository.findActiveByIdForUpdate(SESSION_ID))
                .thenReturn(Optional.of(session));
        stubMessagePersistence();
        stubUsagePersistence();

        var response = service.sendMessage(
                USER_ID,
                SESSION_ID,
                new SendAiMessageRequest("Explain binary trees"));

        assertThat(response.userMessage().role())
                .isEqualTo(AiMessageRole.USER);
        assertThat(response.assistantMessage().role())
                .isEqualTo(AiMessageRole.ASSISTANT);
        assertThat(response.assistantMessage().content())
                .contains("step by step");
        assertThat(response.provider())
                .isEqualTo(LocalStudyAiProvider.PROVIDER_NAME);
        verify(messageRepository, atLeast(2))
                .save(any(AiChatMessage.class));
        verify(usageRepository).save(any(AiUsageRecord.class));
    }

    @Test
    void listMessages_ownedSession_returnsOldestFirstPage() {
        AiChatMessage message = message(
                session,
                AiMessageRole.USER,
                "Explain indexes",
                MESSAGE_ID);
        when(sessionRepository.findActiveById(SESSION_ID))
                .thenReturn(Optional.of(session));
        when(messageRepository.findBySessionId(
                eq(SESSION_ID),
                any(PageRequest.class)))
                .thenReturn(new PageImpl<>(
                        List.of(message),
                        PageRequest.of(0, 20),
                        1));

        var response = service.listMessages(
                USER_ID,
                SESSION_ID,
                0,
                20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().content())
                .isEqualTo("Explain indexes");
    }

    @Test
    void explainConcept_validRequest_returnsLocalExplanationAndTracksUsage() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        stubUsagePersistence();

        var response = service.explainConcept(
                USER_ID,
                new ExplainConceptRequest(
                        "Polymorphism",
                        "Java OOP"));

        assertThat(response.explanation()).contains("step by step");
        assertThat(response.provider())
                .isEqualTo(LocalStudyAiProvider.PROVIDER_NAME);
        verify(usageRepository).save(any(AiUsageRecord.class));
    }

    @Test
    void generators_validRequests_storeStructuredItemsAndUsage() {
        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));
        stubGeneratedItemPersistence();
        stubUsagePersistence();

        var summary = service.generateSummary(
                USER_ID,
                new GenerateSummaryRequest(null, STUDY_TEXT));
        var flashcards = service.generateFlashcards(
                USER_ID,
                new GenerateFlashcardsRequest(
                        "Tree cards",
                        STUDY_TEXT,
                        3));
        var quiz = service.generateQuiz(
                USER_ID,
                new GenerateQuizRequest(
                        "Tree quiz",
                        STUDY_TEXT,
                        2));
        var plan = service.generateStudyPlan(
                USER_ID,
                new GenerateStudyPlanRequest(
                        "Master binary trees",
                        3,
                        60,
                        STUDY_TEXT));

        assertThat(summary.itemType())
                .isEqualTo(AiGeneratedItemType.SUMMARY);
        assertThat(summary.generatedContent().has("keyPoints")).isTrue();
        assertThat(flashcards.generatedContent()).hasSize(3);
        assertThat(quiz.generatedContent()).hasSize(2);
        assertThat(plan.generatedContent().get("plan")).hasSize(3);
        verify(generatedItemRepository, atLeast(4))
                .save(any(AiGeneratedItem.class));
        verify(usageRepository, atLeast(4))
                .save(any(AiUsageRecord.class));
    }

    @Test
    void listGeneratedItems_ownedItems_returnsPage() {
        when(generatedItemRepository.findOwnedItems(
                eq(USER_ID),
                eq(AiGeneratedItemType.SUMMARY),
                any(PageRequest.class)))
                .thenReturn(new PageImpl<>(
                        List.of(generatedItem),
                        PageRequest.of(0, 20),
                        1));

        var response = service.listGeneratedItems(
                USER_ID,
                AiGeneratedItemType.SUMMARY,
                0,
                20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().id())
                .isEqualTo(ITEM_ID);
    }

    @Test
    void getGeneratedItem_ownedItem_returnsItem() {
        when(generatedItemRepository.findActiveById(ITEM_ID))
                .thenReturn(Optional.of(generatedItem));

        var response = service.getGeneratedItem(USER_ID, ITEM_ID);

        assertThat(response.id()).isEqualTo(ITEM_ID);
    }

    @Test
    void getGeneratedItem_differentOwner_rejectsAccess() {
        when(generatedItemRepository.findActiveById(ITEM_ID))
                .thenReturn(Optional.of(generatedItem));

        assertThatThrownBy(() ->
                service.getGeneratedItem(OTHER_USER_ID, ITEM_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteGeneratedItem_ownedItem_softDeletes() {
        when(generatedItemRepository.findActiveByIdForUpdate(ITEM_ID))
                .thenReturn(Optional.of(generatedItem));

        service.deleteGeneratedItem(USER_ID, ITEM_ID);

        assertThat(generatedItem.isDeleted()).isTrue();
    }

    @Test
    void listUsage_ownedRecords_returnsPage() {
        AiUsageRecord usage = new AiUsageRecord(
                user,
                AiUsageFeature.CHAT,
                4,
                10,
                LocalStudyAiProvider.PROVIDER_NAME);
        ReflectionTestUtils.setField(usage, "id", USAGE_ID);
        ReflectionTestUtils.setField(usage, "createdAt", NOW);
        when(usageRepository.findOwnedUsage(
                eq(USER_ID),
                eq(AiUsageFeature.CHAT),
                any(PageRequest.class)))
                .thenReturn(new PageImpl<>(
                        List.of(usage),
                        PageRequest.of(0, 20),
                        1));

        var response = service.listUsage(
                USER_ID,
                AiUsageFeature.CHAT,
                0,
                20);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().provider())
                .isEqualTo(LocalStudyAiProvider.PROVIDER_NAME);
    }

    @Test
    void estimateTokens_usesDeterministicApproximation() {
        assertThat(service.estimateTokens("12345678")).isEqualTo(2);
        assertThat(service.estimateTokens("x")).isEqualTo(1);
        assertThat(service.estimateTokens(null)).isEqualTo(1);
    }

    private User user(UUID id) {
        User result = new User(
                "student@example.com",
                "$2a$12$abcdefghijklmnopqrstuvabcdefghijklmnopqrstuvabcd");
        ReflectionTestUtils.setField(result, "id", id);
        return result;
    }

    private AiChatSession session(User owner) {
        return persistSession(new AiChatSession(
                owner,
                "Binary trees",
                AiSessionMode.GENERAL_CHAT));
    }

    private AiChatSession persistSession(AiChatSession value) {
        ReflectionTestUtils.setField(value, "id", SESSION_ID);
        ReflectionTestUtils.setField(value, "createdAt", NOW);
        ReflectionTestUtils.setField(value, "updatedAt", NOW);
        return value;
    }

    private AiChatMessage message(
            AiChatSession ownerSession,
            AiMessageRole role,
            String content,
            UUID id) {
        AiChatMessage message = new AiChatMessage(
                ownerSession,
                ownerSession.getUser(),
                role,
                content,
                Math.max(1, content.length() / 4));
        ReflectionTestUtils.setField(message, "id", id);
        ReflectionTestUtils.setField(message, "createdAt", NOW);
        return message;
    }

    private AiGeneratedItem generatedItem(
            User owner,
            ObjectMapper objectMapper) {
        AiGeneratedItem item = new AiGeneratedItem(
                owner,
                null,
                AiGeneratedItemType.SUMMARY,
                "Tree summary",
                STUDY_TEXT,
                objectMapper.createObjectNode()
                        .put("shortSummary", "Trees are hierarchical."));
        return persistGeneratedItem(item);
    }

    private AiGeneratedItem persistGeneratedItem(AiGeneratedItem value) {
        ReflectionTestUtils.setField(value, "id", ITEM_ID);
        ReflectionTestUtils.setField(value, "createdAt", NOW);
        ReflectionTestUtils.setField(value, "updatedAt", NOW);
        return value;
    }

    private void stubMessagePersistence() {
        AtomicInteger sequence = new AtomicInteger();
        when(messageRepository.save(any(AiChatMessage.class)))
                .thenAnswer(invocation -> {
                    AiChatMessage message = invocation.getArgument(0);
                    ReflectionTestUtils.setField(
                            message,
                            "id",
                            new UUID(0L, sequence.incrementAndGet()));
                    ReflectionTestUtils.setField(
                            message,
                            "createdAt",
                            NOW);
                    return message;
                });
    }

    private void stubGeneratedItemPersistence() {
        when(generatedItemRepository.save(any(AiGeneratedItem.class)))
                .thenAnswer(invocation -> persistGeneratedItem(
                        invocation.getArgument(0)));
    }

    private void stubUsagePersistence() {
        when(usageRepository.save(any(AiUsageRecord.class)))
                .thenAnswer(invocation -> {
                    AiUsageRecord usage = invocation.getArgument(0);
                    ReflectionTestUtils.setField(usage, "id", USAGE_ID);
                    ReflectionTestUtils.setField(
                            usage,
                            "createdAt",
                            NOW);
                    return usage;
                });
    }
}
