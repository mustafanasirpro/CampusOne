package com.campusone.discussion.controller;

import com.campusone.discussion.dto.request.CreateAnswerRequest;
import com.campusone.discussion.dto.request.CreateQuestionRequest;
import com.campusone.discussion.dto.request.QuestionSort;
import com.campusone.discussion.dto.request.UpdateAnswerRequest;
import com.campusone.discussion.dto.request.UpdateQuestionRequest;
import com.campusone.discussion.dto.request.VoteRequest;
import com.campusone.discussion.dto.response.AnswerPageResponse;
import com.campusone.discussion.dto.response.AnswerResponse;
import com.campusone.discussion.dto.response.QuestionDetailResponse;
import com.campusone.discussion.dto.response.QuestionPageResponse;
import com.campusone.discussion.dto.response.VoteResponse;
import com.campusone.discussion.entity.DiscussionCategory;
import com.campusone.discussion.service.DiscussionService;
import com.campusone.security.CampusOneUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/discussions")
@Validated
@Tag(name = "Discussions")
public class DiscussionController {

    private final DiscussionService discussionService;

    public DiscussionController(DiscussionService discussionService) {
        this.discussionService = discussionService;
    }

    @PostMapping("/questions")
    @Operation(summary = "Create a discussion question")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<QuestionDetailResponse> createQuestion(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody CreateQuestionRequest request) {
        QuestionDetailResponse response =
                discussionService.createQuestion(
                        principal.getUserId(),
                        request);
        return ResponseEntity.created(
                        URI.create("/api/v1/discussions/questions/"
                                + response.id()))
                .body(response);
    }

    @GetMapping("/questions")
    @Operation(summary = "List visible discussion questions")
    public ResponseEntity<QuestionPageResponse> listQuestions(
            @RequestParam(required = false) DiscussionCategory category,
            @RequestParam(required = false) @Size(max = 200) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "NEWEST") QuestionSort sort) {
        return ResponseEntity.ok(discussionService.listQuestions(
                category,
                search,
                page,
                size,
                sort));
    }

    @GetMapping("/questions/my")
    @Operation(summary = "List the authenticated student's questions")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<QuestionPageResponse> listMyQuestions(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "NEWEST") QuestionSort sort) {
        return ResponseEntity.ok(discussionService.listMyQuestions(
                principal.getUserId(),
                page,
                size,
                sort));
    }

    @GetMapping("/questions/{questionId}")
    @Operation(summary = "Get a discussion question with its first answer page")
    public ResponseEntity<QuestionDetailResponse> getQuestion(
            @PathVariable UUID questionId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        UUID viewerUserId = principal == null ? null : principal.getUserId();
        return ResponseEntity.ok(discussionService.getQuestion(
                questionId,
                viewerUserId));
    }

    @PatchMapping("/questions/{questionId}")
    @Operation(summary = "Update an owned discussion question")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<QuestionDetailResponse> updateQuestion(
            @PathVariable UUID questionId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody UpdateQuestionRequest request) {
        return ResponseEntity.ok(discussionService.updateQuestion(
                principal.getUserId(),
                questionId,
                request));
    }

    @DeleteMapping("/questions/{questionId}")
    @Operation(summary = "Soft-delete an owned discussion question")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable UUID questionId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        discussionService.deleteQuestion(
                principal.getUserId(),
                questionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/questions/{questionId}/answers")
    @Operation(summary = "Create an answer for a discussion question")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<AnswerResponse> createAnswer(
            @PathVariable UUID questionId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody CreateAnswerRequest request) {
        AnswerResponse response = discussionService.createAnswer(
                principal.getUserId(),
                questionId,
                request);
        return ResponseEntity.created(
                        URI.create("/api/v1/discussions/answers/"
                                + response.id()))
                .body(response);
    }

    @GetMapping("/questions/{questionId}/answers")
    @Operation(summary = "List visible answers for a discussion question")
    public ResponseEntity<AnswerPageResponse> listAnswers(
            @PathVariable UUID questionId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        UUID viewerUserId = principal == null ? null : principal.getUserId();
        return ResponseEntity.ok(discussionService.listAnswers(
                questionId,
                viewerUserId,
                page,
                size));
    }

    @PatchMapping("/answers/{answerId}")
    @Operation(summary = "Update an owned discussion answer")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<AnswerResponse> updateAnswer(
            @PathVariable UUID answerId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody UpdateAnswerRequest request) {
        return ResponseEntity.ok(discussionService.updateAnswer(
                principal.getUserId(),
                answerId,
                request));
    }

    @DeleteMapping("/answers/{answerId}")
    @Operation(summary = "Soft-delete an owned discussion answer")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteAnswer(
            @PathVariable UUID answerId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        discussionService.deleteAnswer(
                principal.getUserId(),
                answerId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/questions/{questionId}/vote")
    @Operation(summary = "Create or replace the current user's question vote")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<VoteResponse> voteQuestion(
            @PathVariable UUID questionId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody VoteRequest request) {
        return ResponseEntity.ok(discussionService.voteQuestion(
                principal.getUserId(),
                questionId,
                request.voteValue()));
    }

    @DeleteMapping("/questions/{questionId}/vote")
    @Operation(summary = "Remove the current user's question vote")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<VoteResponse> removeQuestionVote(
            @PathVariable UUID questionId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(discussionService.removeQuestionVote(
                principal.getUserId(),
                questionId));
    }

    @PutMapping("/answers/{answerId}/vote")
    @Operation(summary = "Create or replace the current user's answer vote")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<VoteResponse> voteAnswer(
            @PathVariable UUID answerId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody VoteRequest request) {
        return ResponseEntity.ok(discussionService.voteAnswer(
                principal.getUserId(),
                answerId,
                request.voteValue()));
    }

    @DeleteMapping("/answers/{answerId}/vote")
    @Operation(summary = "Remove the current user's answer vote")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<VoteResponse> removeAnswerVote(
            @PathVariable UUID answerId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(discussionService.removeAnswerVote(
                principal.getUserId(),
                answerId));
    }

    @PutMapping("/questions/{questionId}/accepted-answer/{answerId}")
    @Operation(summary = "Accept an answer to an owned question")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<QuestionDetailResponse> acceptAnswer(
            @PathVariable UUID questionId,
            @PathVariable UUID answerId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(discussionService.acceptAnswer(
                principal.getUserId(),
                questionId,
                answerId));
    }

    @DeleteMapping("/questions/{questionId}/accepted-answer")
    @Operation(summary = "Remove the accepted answer from an owned question")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<QuestionDetailResponse> unacceptAnswer(
            @PathVariable UUID questionId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(discussionService.unacceptAnswer(
                principal.getUserId(),
                questionId));
    }
}
