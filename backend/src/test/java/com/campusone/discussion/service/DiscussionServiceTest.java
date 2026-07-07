package com.campusone.discussion.service;

import com.campusone.common.service.CommunityIntegrationService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.common.exception.ResourceNotFoundException;
import com.campusone.discussion.dto.request.CreateAnswerRequest;
import com.campusone.discussion.dto.request.CreateQuestionRequest;
import com.campusone.discussion.dto.request.QuestionSort;
import com.campusone.discussion.dto.request.QuestionUpdateStatus;
import com.campusone.discussion.dto.request.UpdateAnswerRequest;
import com.campusone.discussion.dto.request.UpdateQuestionRequest;
import com.campusone.discussion.entity.DiscussionAnswer;
import com.campusone.discussion.entity.DiscussionAnswerVote;
import com.campusone.discussion.entity.DiscussionAnswerVoteId;
import com.campusone.discussion.entity.DiscussionCategory;
import com.campusone.discussion.entity.DiscussionQuestion;
import com.campusone.discussion.entity.DiscussionQuestionStatus;
import com.campusone.discussion.entity.DiscussionQuestionVote;
import com.campusone.discussion.entity.DiscussionQuestionVoteId;
import com.campusone.discussion.mapper.DiscussionMapper;
import com.campusone.discussion.repository.DiscussionAnswerRepository;
import com.campusone.discussion.repository.DiscussionAnswerVoteRepository;
import com.campusone.discussion.repository.DiscussionQuestionRepository;
import com.campusone.discussion.repository.DiscussionQuestionVoteRepository;
import com.campusone.note.service.NoteAdminAuthorizationService;
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DiscussionServiceTest {

    private static final UUID OWNER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000002");
    private static final UUID QUESTION_ID = UUID.fromString(
            "30000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_QUESTION_ID = UUID.fromString(
            "30000000-0000-4000-8000-000000000002");
    private static final UUID ANSWER_ID = UUID.fromString(
            "40000000-0000-4000-8000-000000000001");
    private static final Instant NOW = Instant.parse("2026-07-02T12:00:00Z");

    @Mock
    private DiscussionQuestionRepository questionRepository;

    @Mock
    private DiscussionAnswerRepository answerRepository;

    @Mock
    private DiscussionQuestionVoteRepository questionVoteRepository;

    @Mock
    private DiscussionAnswerVoteRepository answerVoteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommunityIntegrationService integrationService;

    @Mock
    private NoteAdminAuthorizationService adminAuthorizationService;

    private DiscussionService discussionService;
    private User owner;
    private User otherUser;
    private DiscussionQuestion question;
    private DiscussionAnswer answer;

    @BeforeEach
    void setUp() {
        owner = user(OWNER_ID, "owner@example.com");
        otherUser = user(OTHER_USER_ID, "other@example.com");
        question = question(owner, QUESTION_ID);
        answer = answer(question, otherUser, ANSWER_ID);
        discussionService = new DiscussionService(
                questionRepository,
                answerRepository,
                questionVoteRepository,
                answerVoteRepository,
                userRepository,
                new DiscussionMapper(),
                integrationService,
                adminAuthorizationService);

        lenient().when(answerRepository.findVisibleByQuestionId(
                        any(UUID.class),
                        any(Pageable.class)))
                .thenAnswer(invocation ->
                        Page.empty(invocation.getArgument(1)));
        lenient().when(answerVoteRepository.findForAnswersAndUser(
                        any(),
                        any(UUID.class)))
                .thenReturn(List.of());
        lenient().when(adminAuthorizationService.canManage(
                        any(UUID.class),
                        any()))
                .thenReturn(true);
    }

    @Test
    void createQuestion_validRequest_createsOpenQuestion() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(questionRepository.save(any(DiscussionQuestion.class)))
                .thenAnswer(invocation -> {
                    DiscussionQuestion saved = invocation.getArgument(0);
                    setQuestionPersistenceFields(saved, QUESTION_ID);
                    return saved;
                });

        var response = discussionService.createQuestion(
                OWNER_ID,
                createQuestionRequest());

        assertThat(response.id()).isEqualTo(QUESTION_ID);
        assertThat(response.status()).isEqualTo(DiscussionQuestionStatus.OPEN);
        assertThat(response.ownedByCurrentUser()).isTrue();
    }

    @Test
    void listQuestions_returnsVisiblePage() {
        when(questionRepository.findVisibleQuestions(
                any(),
                eq(DiscussionCategory.PROGRAMMING),
                eq("%spring%"),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(question)));

        var response = discussionService.listQuestions(
                DiscussionCategory.PROGRAMMING,
                " Spring ",
                0,
                20,
                QuestionSort.NEWEST);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().id()).isEqualTo(QUESTION_ID);
    }

    @Test
    void getQuestion_visibleQuestion_incrementsViewCount() {
        when(questionRepository.findActiveByIdForUpdate(QUESTION_ID))
                .thenReturn(Optional.of(question));

        var response = discussionService.getQuestion(QUESTION_ID, null);

        assertThat(response.viewCount()).isEqualTo(1);
        assertThat(response.ownedByCurrentUser()).isFalse();
    }

    @Test
    void updateQuestion_ownerCanUpdate() {
        when(questionRepository.findActiveByIdForUpdate(QUESTION_ID))
                .thenReturn(Optional.of(question));

        var response = discussionService.updateQuestion(
                OWNER_ID,
                QUESTION_ID,
                new UpdateQuestionRequest(
                        "Updated dependency injection question",
                        null,
                        DiscussionCategory.ACADEMIC,
                        QuestionUpdateStatus.CLOSED));

        assertThat(response.title())
                .isEqualTo("Updated dependency injection question");
        assertThat(response.category())
                .isEqualTo(DiscussionCategory.ACADEMIC);
        assertThat(response.status())
                .isEqualTo(DiscussionQuestionStatus.CLOSED);
    }

    @Test
    void updateQuestion_nonOwnerIsRejected() {
        when(questionRepository.findActiveByIdForUpdate(QUESTION_ID))
                .thenReturn(Optional.of(question));

        assertThatThrownBy(() -> discussionService.updateQuestion(
                OTHER_USER_ID,
                QUESTION_ID,
                new UpdateQuestionRequest(
                        "Unauthorized question update",
                        null,
                        null,
                        null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteQuestion_ownerSoftDeletesQuestion() {
        when(questionRepository.findActiveByIdForUpdate(QUESTION_ID))
                .thenReturn(Optional.of(question));

        discussionService.deleteQuestion(OWNER_ID, QUESTION_ID);

        assertThat(question.isDeleted()).isTrue();
    }

    @Test
    void createAnswer_validRequest_incrementsAnswerCount() {
        when(questionRepository.findActiveByIdForUpdate(QUESTION_ID))
                .thenReturn(Optional.of(question));
        when(userRepository.findById(OTHER_USER_ID))
                .thenReturn(Optional.of(otherUser));
        when(answerRepository.save(any(DiscussionAnswer.class)))
                .thenAnswer(invocation -> {
                    DiscussionAnswer saved = invocation.getArgument(0);
                    setAnswerPersistenceFields(saved, ANSWER_ID);
                    return saved;
                });

        var response = discussionService.createAnswer(
                OTHER_USER_ID,
                QUESTION_ID,
                new CreateAnswerRequest(
                        "Constructor injection makes dependencies explicit."));

        assertThat(response.id()).isEqualTo(ANSWER_ID);
        assertThat(question.getAnswerCount()).isEqualTo(1);
    }

    @Test
    void updateAnswer_ownerCanUpdate() {
        stubAnswerMutationContext();

        var response = discussionService.updateAnswer(
                OTHER_USER_ID,
                ANSWER_ID,
                new UpdateAnswerRequest(
                        "Constructor injection also improves testability."));

        assertThat(response.body())
                .isEqualTo("Constructor injection also improves testability.");
    }

    @Test
    void updateAnswer_nonOwnerIsRejected() {
        stubAnswerMutationContext();

        assertThatThrownBy(() -> discussionService.updateAnswer(
                OWNER_ID,
                ANSWER_ID,
                new UpdateAnswerRequest(
                        "This update must not be accepted by the service.")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void voteQuestion_newVote_updatesScore() {
        when(questionRepository.findActiveByIdForUpdate(QUESTION_ID))
                .thenReturn(Optional.of(question));
        when(userRepository.findById(OTHER_USER_ID))
                .thenReturn(Optional.of(otherUser));

        var response = discussionService.voteQuestion(
                OTHER_USER_ID,
                QUESTION_ID,
                1);

        assertThat(response.voteScore()).isEqualTo(1);
        verify(questionVoteRepository).save(
                any(DiscussionQuestionVote.class));
    }

    @Test
    void voteQuestion_changedVote_appliesOnlyDelta() {
        DiscussionQuestionVote vote =
                new DiscussionQuestionVote(question, otherUser, 1);
        when(questionRepository.findActiveByIdForUpdate(QUESTION_ID))
                .thenReturn(Optional.of(question));
        when(userRepository.findById(OTHER_USER_ID))
                .thenReturn(Optional.of(otherUser));
        when(questionVoteRepository.findById(
                new DiscussionQuestionVoteId(
                        QUESTION_ID,
                        OTHER_USER_ID)))
                .thenReturn(Optional.of(vote));
        question.applyVoteChange(1);

        var response = discussionService.voteQuestion(
                OTHER_USER_ID,
                QUESTION_ID,
                -1);

        assertThat(response.voteScore()).isEqualTo(-1);
        assertThat(vote.getVoteValue()).isEqualTo(-1);
    }

    @Test
    void removeQuestionVote_existingVote_reversesScore() {
        DiscussionQuestionVote vote =
                new DiscussionQuestionVote(question, otherUser, 1);
        when(questionRepository.findActiveByIdForUpdate(QUESTION_ID))
                .thenReturn(Optional.of(question));
        when(questionVoteRepository.findById(
                new DiscussionQuestionVoteId(
                        QUESTION_ID,
                        OTHER_USER_ID)))
                .thenReturn(Optional.of(vote));
        question.applyVoteChange(1);

        var response = discussionService.removeQuestionVote(
                OTHER_USER_ID,
                QUESTION_ID);

        assertThat(response.currentUserVote()).isNull();
        assertThat(response.voteScore()).isZero();
        verify(questionVoteRepository).delete(vote);
    }

    @Test
    void voteAnswer_newVote_updatesScore() {
        stubAnswerMutationContext();
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));

        var response = discussionService.voteAnswer(
                OWNER_ID,
                ANSWER_ID,
                1);

        assertThat(response.voteScore()).isEqualTo(1);
        verify(answerVoteRepository).save(any(DiscussionAnswerVote.class));
    }

    @Test
    void acceptAnswer_questionOwnerAcceptsMatchingAnswer() {
        when(questionRepository.findActiveByIdForUpdate(QUESTION_ID))
                .thenReturn(Optional.of(question));
        when(answerRepository.findActiveByIdAndQuestionIdForUpdate(
                ANSWER_ID,
                QUESTION_ID))
                .thenReturn(Optional.of(answer));

        var response = discussionService.acceptAnswer(
                OWNER_ID,
                QUESTION_ID,
                ANSWER_ID);

        assertThat(response.status())
                .isEqualTo(DiscussionQuestionStatus.RESOLVED);
        assertThat(response.acceptedAnswerId()).isEqualTo(ANSWER_ID);
        assertThat(answer.isAccepted()).isTrue();
    }

    @Test
    void acceptAnswer_nonOwnerIsRejected() {
        when(questionRepository.findActiveByIdForUpdate(QUESTION_ID))
                .thenReturn(Optional.of(question));

        assertThatThrownBy(() -> discussionService.acceptAnswer(
                OTHER_USER_ID,
                QUESTION_ID,
                ANSWER_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void acceptAnswer_answerFromDifferentQuestionIsRejected() {
        when(questionRepository.findActiveByIdForUpdate(QUESTION_ID))
                .thenReturn(Optional.of(question));
        when(answerRepository.findActiveByIdAndQuestionIdForUpdate(
                ANSWER_ID,
                QUESTION_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> discussionService.acceptAnswer(
                OWNER_ID,
                QUESTION_ID,
                ANSWER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteAnswer_acceptedAnswer_clearsAcceptanceAndDecrementsCount() {
        question.incrementAnswerCount();
        question.acceptAnswer(answer);
        stubAnswerMutationContext();

        discussionService.deleteAnswer(
                OTHER_USER_ID,
                ANSWER_ID);

        assertThat(answer.isDeleted()).isTrue();
        assertThat(answer.isAccepted()).isFalse();
        assertThat(question.getAcceptedAnswerId()).isNull();
        assertThat(question.getStatus()).isEqualTo(DiscussionQuestionStatus.OPEN);
        assertThat(question.getAnswerCount()).isZero();
    }

    private void stubAnswerMutationContext() {
        when(answerRepository.findActiveById(ANSWER_ID))
                .thenReturn(Optional.of(answer));
        when(questionRepository.findActiveByIdForUpdate(QUESTION_ID))
                .thenReturn(Optional.of(question));
        when(answerRepository.findActiveByIdAndQuestionIdForUpdate(
                ANSWER_ID,
                QUESTION_ID))
                .thenReturn(Optional.of(answer));
    }

    private CreateQuestionRequest createQuestionRequest() {
        return new CreateQuestionRequest(
                "How does dependency injection work?",
                "I understand the basic idea but need a practical explanation.",
                DiscussionCategory.PROGRAMMING);
    }

    private DiscussionQuestion question(User author, UUID id) {
        DiscussionQuestion result = new DiscussionQuestion(
                author,
                "How does dependency injection work?",
                "I understand the basic idea but need a practical explanation.",
                DiscussionCategory.PROGRAMMING);
        setQuestionPersistenceFields(result, id);
        return result;
    }

    private DiscussionAnswer answer(
            DiscussionQuestion parent,
            User author,
            UUID id) {
        DiscussionAnswer result = new DiscussionAnswer(
                parent,
                author,
                "Constructor injection makes dependencies explicit.");
        setAnswerPersistenceFields(result, id);
        return result;
    }

    private User user(UUID id, String email) {
        User user = new User(email, "$2a$12$encoded-password");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private void setQuestionPersistenceFields(
            DiscussionQuestion target,
            UUID id) {
        ReflectionTestUtils.setField(target, "id", id);
        ReflectionTestUtils.setField(target, "createdAt", NOW);
        ReflectionTestUtils.setField(target, "updatedAt", NOW);
    }

    private void setAnswerPersistenceFields(
            DiscussionAnswer target,
            UUID id) {
        ReflectionTestUtils.setField(target, "id", id);
        ReflectionTestUtils.setField(target, "createdAt", NOW);
        ReflectionTestUtils.setField(target, "updatedAt", NOW);
    }
}
