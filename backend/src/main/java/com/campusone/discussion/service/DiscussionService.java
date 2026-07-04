package com.campusone.discussion.service;

import com.campusone.common.exception.ResourceNotFoundException;
import com.campusone.common.service.CommunityIntegrationService;
import com.campusone.discussion.dto.request.CreateAnswerRequest;
import com.campusone.discussion.dto.request.CreateQuestionRequest;
import com.campusone.discussion.dto.request.QuestionSort;
import com.campusone.discussion.dto.request.UpdateAnswerRequest;
import com.campusone.discussion.dto.request.UpdateQuestionRequest;
import com.campusone.discussion.dto.response.AnswerPageResponse;
import com.campusone.discussion.dto.response.AnswerResponse;
import com.campusone.discussion.dto.response.QuestionDetailResponse;
import com.campusone.discussion.dto.response.QuestionPageResponse;
import com.campusone.discussion.dto.response.QuestionSummaryResponse;
import com.campusone.discussion.dto.response.VoteResponse;
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
import com.campusone.user.entity.User;
import com.campusone.user.repository.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiscussionService {

    private static final int DETAIL_ANSWER_PAGE_SIZE = 20;

    private final DiscussionQuestionRepository questionRepository;
    private final DiscussionAnswerRepository answerRepository;
    private final DiscussionQuestionVoteRepository questionVoteRepository;
    private final DiscussionAnswerVoteRepository answerVoteRepository;
    private final UserRepository userRepository;
    private final DiscussionMapper discussionMapper;
    private final CommunityIntegrationService integrationService;

    public DiscussionService(
            DiscussionQuestionRepository questionRepository,
            DiscussionAnswerRepository answerRepository,
            DiscussionQuestionVoteRepository questionVoteRepository,
            DiscussionAnswerVoteRepository answerVoteRepository,
            UserRepository userRepository,
            DiscussionMapper discussionMapper,
            CommunityIntegrationService integrationService) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.questionVoteRepository = questionVoteRepository;
        this.answerVoteRepository = answerVoteRepository;
        this.userRepository = userRepository;
        this.discussionMapper = discussionMapper;
        this.integrationService = integrationService;
    }

    @Transactional
    public QuestionDetailResponse createQuestion(
            UUID userId,
            CreateQuestionRequest request) {
        User author = requireUser(userId);
        DiscussionQuestion question = questionRepository.save(
                new DiscussionQuestion(
                        author,
                        request.title(),
                        request.body(),
                        request.category()));
        integrationService.discussionQuestionCreated(userId, question.getId());
        return discussionMapper.toQuestionDetail(
                question,
                null,
                true,
                emptyAnswerPage());
    }

    @Transactional(readOnly = true)
    public QuestionPageResponse listQuestions(
            DiscussionCategory category,
            String search,
            int page,
            int size,
            QuestionSort sort) {
        Page<DiscussionQuestion> questions =
                questionRepository.findVisibleQuestions(
                        DiscussionQuestionStatus.HIDDEN,
                        category,
                        toSearchPattern(search),
                        PageRequest.of(page, size, sort.toSort()));
        return toQuestionPage(questions);
    }

    @Transactional(readOnly = true)
    public QuestionPageResponse listMyQuestions(
            UUID userId,
            int page,
            int size,
            QuestionSort sort) {
        Page<DiscussionQuestion> questions =
                questionRepository.findMyQuestions(
                        userId,
                        PageRequest.of(page, size, sort.toSort()));
        return toQuestionPage(questions);
    }

    @Transactional
    public QuestionDetailResponse getQuestion(
            UUID questionId,
            UUID viewerUserId) {
        DiscussionQuestion question = requireQuestionForUpdate(questionId);
        requireViewable(question, viewerUserId);
        question.recordView();
        return toQuestionDetail(question, viewerUserId);
    }

    @Transactional
    public QuestionDetailResponse updateQuestion(
            UUID userId,
            UUID questionId,
            UpdateQuestionRequest request) {
        DiscussionQuestion question = requireQuestionForUpdate(questionId);
        requireOwner(question, userId);

        if (request.status() != null
                && question.getAcceptedAnswerId() != null) {
            requireAnswerForUpdate(
                    question.getAcceptedAnswerId(),
                    question.getId());
        }

        question.update(
                request.title(),
                request.body(),
                request.category(),
                request.status() == null
                        ? null
                        : request.status().toQuestionStatus());
        return toQuestionDetail(question, userId);
    }

    @Transactional
    public void deleteQuestion(UUID userId, UUID questionId) {
        DiscussionQuestion question = requireQuestionForUpdate(questionId);
        requireOwner(question, userId);
        question.softDelete();
    }

    @Transactional
    public AnswerResponse createAnswer(
            UUID userId,
            UUID questionId,
            CreateAnswerRequest request) {
        DiscussionQuestion question = requireQuestionForUpdate(questionId);
        requireViewable(question, userId);
        User author = requireUser(userId);
        DiscussionAnswer answer = answerRepository.save(
                new DiscussionAnswer(question, author, request.body()));
        question.incrementAnswerCount();
        integrationService.discussionAnswerCreated(
                userId,
                question.getAuthor().getId(),
                questionId,
                answer.getId());
        return discussionMapper.toAnswer(answer, null, true);
    }

    @Transactional
    public AnswerResponse updateAnswer(
            UUID userId,
            UUID answerId,
            UpdateAnswerRequest request) {
        DiscussionAnswer initialAnswer = requireAnswer(answerId);
        DiscussionQuestion question = requireQuestionForUpdate(
                initialAnswer.getQuestion().getId());
        requireViewable(question, userId);
        DiscussionAnswer answer = requireAnswerForUpdate(
                answerId,
                question.getId());
        requireOwner(answer, userId);
        answer.updateBody(request.body());
        return discussionMapper.toAnswer(
                answer,
                currentAnswerVote(answerId, userId),
                true);
    }

    @Transactional
    public void deleteAnswer(UUID userId, UUID answerId) {
        DiscussionAnswer initialAnswer = requireAnswer(answerId);
        DiscussionQuestion question = requireQuestionForUpdate(
                initialAnswer.getQuestion().getId());
        requireViewable(question, userId);
        DiscussionAnswer answer = requireAnswerForUpdate(
                answerId,
                question.getId());
        requireOwner(answer, userId);

        if (answer.isAccepted()) {
            question.unacceptAnswer();
        }
        answer.softDelete();
        question.decrementAnswerCount();
    }

    @Transactional(readOnly = true)
    public AnswerPageResponse listAnswers(
            UUID questionId,
            UUID viewerUserId,
            int page,
            int size) {
        DiscussionQuestion question = requireQuestion(questionId);
        requireViewable(question, viewerUserId);
        return loadAnswers(questionId, viewerUserId, page, size);
    }

    @Transactional
    public VoteResponse voteQuestion(
            UUID userId,
            UUID questionId,
            int voteValue) {
        DiscussionQuestion question = requireQuestionForUpdate(questionId);
        requireViewable(question, userId);
        requireNotSelfVote(question.isOwnedBy(userId));
        User user = requireUser(userId);
        DiscussionQuestionVoteId voteId =
                new DiscussionQuestionVoteId(questionId, userId);
        DiscussionQuestionVote vote =
                questionVoteRepository.findById(voteId).orElse(null);
        int previousVote = vote == null ? 0 : vote.getVoteValue();

        if (vote == null) {
            questionVoteRepository.save(
                    new DiscussionQuestionVote(question, user, voteValue));
        } else {
            vote.updateVote(voteValue);
        }
        question.applyVoteChange(voteValue - previousVote);
        return new VoteResponse(
                questionId,
                voteValue,
                question.getVoteScore());
    }

    @Transactional
    public VoteResponse removeQuestionVote(
            UUID userId,
            UUID questionId) {
        DiscussionQuestion question = requireQuestionForUpdate(questionId);
        requireViewable(question, userId);
        DiscussionQuestionVoteId voteId =
                new DiscussionQuestionVoteId(questionId, userId);
        DiscussionQuestionVote vote =
                questionVoteRepository.findById(voteId).orElse(null);
        if (vote != null) {
            questionVoteRepository.delete(vote);
            question.applyVoteChange(-vote.getVoteValue());
        }
        return new VoteResponse(
                questionId,
                null,
                question.getVoteScore());
    }

    @Transactional
    public VoteResponse voteAnswer(
            UUID userId,
            UUID answerId,
            int voteValue) {
        DiscussionAnswer initialAnswer = requireAnswer(answerId);
        DiscussionQuestion question = requireQuestionForUpdate(
                initialAnswer.getQuestion().getId());
        requireViewable(question, userId);
        DiscussionAnswer answer = requireAnswerForUpdate(
                answerId,
                question.getId());
        requireNotSelfVote(answer.isOwnedBy(userId));
        User user = requireUser(userId);
        DiscussionAnswerVoteId voteId =
                new DiscussionAnswerVoteId(answerId, userId);
        DiscussionAnswerVote vote =
                answerVoteRepository.findById(voteId).orElse(null);
        int previousVote = vote == null ? 0 : vote.getVoteValue();

        if (vote == null) {
            answerVoteRepository.save(
                    new DiscussionAnswerVote(answer, user, voteValue));
        } else {
            vote.updateVote(voteValue);
        }
        answer.applyVoteChange(voteValue - previousVote);
        return new VoteResponse(
                answerId,
                voteValue,
                answer.getVoteScore());
    }

    @Transactional
    public VoteResponse removeAnswerVote(
            UUID userId,
            UUID answerId) {
        DiscussionAnswer initialAnswer = requireAnswer(answerId);
        DiscussionQuestion question = requireQuestionForUpdate(
                initialAnswer.getQuestion().getId());
        requireViewable(question, userId);
        DiscussionAnswer answer = requireAnswerForUpdate(
                answerId,
                question.getId());
        DiscussionAnswerVoteId voteId =
                new DiscussionAnswerVoteId(answerId, userId);
        DiscussionAnswerVote vote =
                answerVoteRepository.findById(voteId).orElse(null);
        if (vote != null) {
            answerVoteRepository.delete(vote);
            answer.applyVoteChange(-vote.getVoteValue());
        }
        return new VoteResponse(
                answerId,
                null,
                answer.getVoteScore());
    }

    @Transactional
    public QuestionDetailResponse acceptAnswer(
            UUID userId,
            UUID questionId,
            UUID answerId) {
        DiscussionQuestion question = requireQuestionForUpdate(questionId);
        requireOwner(question, userId);
        DiscussionAnswer answer = requireAnswerForUpdate(answerId, questionId);
        UUID previousAcceptedAnswerId = question.getAcceptedAnswerId();
        if (previousAcceptedAnswerId != null
                && !previousAcceptedAnswerId.equals(answerId)) {
            requireAnswerForUpdate(previousAcceptedAnswerId, questionId);
        }
        question.acceptAnswer(answer);
        if (!answerId.equals(previousAcceptedAnswerId)) {
            integrationService.discussionAnswerAccepted(
                    answer.getAuthor().getId(),
                    question.getAuthor().getId(),
                    questionId,
                    answerId);
        }
        return toQuestionDetail(question, userId);
    }

    @Transactional
    public QuestionDetailResponse unacceptAnswer(
            UUID userId,
            UUID questionId) {
        DiscussionQuestion question = requireQuestionForUpdate(questionId);
        requireOwner(question, userId);
        if (question.getAcceptedAnswerId() != null) {
            requireAnswerForUpdate(
                    question.getAcceptedAnswerId(),
                    questionId);
        }
        question.unacceptAnswer();
        return toQuestionDetail(question, userId);
    }

    private QuestionDetailResponse toQuestionDetail(
            DiscussionQuestion question,
            UUID viewerUserId) {
        AnswerPageResponse answers = loadAnswers(
                question.getId(),
                viewerUserId,
                0,
                DETAIL_ANSWER_PAGE_SIZE);
        Integer currentUserVote = viewerUserId == null
                ? null
                : questionVoteRepository.findById(
                                new DiscussionQuestionVoteId(
                                        question.getId(),
                                        viewerUserId))
                        .map(DiscussionQuestionVote::getVoteValue)
                        .orElse(null);
        return discussionMapper.toQuestionDetail(
                question,
                currentUserVote,
                viewerUserId != null && question.isOwnedBy(viewerUserId),
                answers);
    }

    private QuestionPageResponse toQuestionPage(
            Page<DiscussionQuestion> page) {
        List<QuestionSummaryResponse> content = page.getContent().stream()
                .map(discussionMapper::toQuestionSummary)
                .toList();
        return new QuestionPageResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }

    private AnswerPageResponse loadAnswers(
            UUID questionId,
            UUID viewerUserId,
            int page,
            int size) {
        Page<DiscussionAnswer> answers =
                answerRepository.findVisibleByQuestionId(
                        questionId,
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(
                                        Sort.Order.desc("accepted"),
                                        Sort.Order.asc("createdAt"),
                                        Sort.Order.asc("id"))));
        Map<UUID, Integer> votes = answerVotes(
                answers.getContent(),
                viewerUserId);
        List<AnswerResponse> content = answers.getContent().stream()
                .map(answer -> discussionMapper.toAnswer(
                        answer,
                        votes.get(answer.getId()),
                        viewerUserId != null
                                && answer.isOwnedBy(viewerUserId)))
                .toList();
        return new AnswerPageResponse(
                content,
                answers.getNumber(),
                answers.getSize(),
                answers.getTotalElements(),
                answers.getTotalPages(),
                answers.isFirst(),
                answers.isLast());
    }

    private Map<UUID, Integer> answerVotes(
            List<DiscussionAnswer> answers,
            UUID viewerUserId) {
        if (viewerUserId == null || answers.isEmpty()) {
            return Map.of();
        }
        List<UUID> answerIds = answers.stream()
                .map(DiscussionAnswer::getId)
                .toList();
        Map<UUID, Integer> votes = new HashMap<>();
        answerVoteRepository.findForAnswersAndUser(answerIds, viewerUserId)
                .forEach(vote -> votes.put(
                        vote.getId().getAnswerId(),
                        vote.getVoteValue()));
        return votes;
    }

    private Integer currentAnswerVote(UUID answerId, UUID userId) {
        return answerVoteRepository.findById(
                        new DiscussionAnswerVoteId(answerId, userId))
                .map(DiscussionAnswerVote::getVoteValue)
                .orElse(null);
    }

    private AnswerPageResponse emptyAnswerPage() {
        return new AnswerPageResponse(
                List.of(),
                0,
                DETAIL_ANSWER_PAGE_SIZE,
                0,
                0,
                true,
                true);
    }

    private String toSearchPattern(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String escaped = search.trim()
                .toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));
    }

    private DiscussionQuestion requireQuestion(UUID questionId) {
        return questionRepository.findActiveById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Discussion question"));
    }

    private DiscussionQuestion requireQuestionForUpdate(UUID questionId) {
        return questionRepository.findActiveByIdForUpdate(questionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Discussion question"));
    }

    private DiscussionAnswer requireAnswer(UUID answerId) {
        return answerRepository.findActiveById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Discussion answer"));
    }

    private DiscussionAnswer requireAnswerForUpdate(
            UUID answerId,
            UUID questionId) {
        return answerRepository.findActiveByIdAndQuestionIdForUpdate(
                        answerId,
                        questionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Discussion answer"));
    }

    private void requireViewable(
            DiscussionQuestion question,
            UUID viewerUserId) {
        if (question.getStatus() == DiscussionQuestionStatus.HIDDEN
                && (viewerUserId == null
                        || !question.isOwnedBy(viewerUserId))) {
            throw new ResourceNotFoundException("Discussion question");
        }
    }

    private void requireOwner(
            DiscussionQuestion question,
            UUID userId) {
        if (!question.isOwnedBy(userId)) {
            throw new AccessDeniedException(
                    "Only the question owner may modify this question.");
        }
    }

    private void requireOwner(
            DiscussionAnswer answer,
            UUID userId) {
        if (!answer.isOwnedBy(userId)) {
            throw new AccessDeniedException(
                    "Only the answer owner may modify this answer.");
        }
    }

    private void requireNotSelfVote(boolean ownedByCurrentUser) {
        if (ownedByCurrentUser) {
            throw new AccessDeniedException(
                    "Users cannot vote on their own discussion content.");
        }
    }
}
