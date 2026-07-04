package com.campusone.ai.controller;

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
import com.campusone.ai.dto.response.AiSessionDetailResponse;
import com.campusone.ai.dto.response.AiSessionPageResponse;
import com.campusone.ai.dto.response.AiSessionSummaryResponse;
import com.campusone.ai.dto.response.AiUsagePageResponse;
import com.campusone.ai.entity.AiGeneratedItemType;
import com.campusone.ai.entity.AiSessionMode;
import com.campusone.ai.entity.AiUsageFeature;
import com.campusone.ai.service.AiAssistantService;
import com.campusone.security.CampusOneUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@Validated
@Tag(name = "AI Study Assistant")
@SecurityRequirement(name = "bearerAuth")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    public AiAssistantController(
            AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @PostMapping("/sessions")
    @Operation(summary = "Create an AI chat session")
    public ResponseEntity<AiSessionSummaryResponse> createSession(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody CreateAiSessionRequest request) {
        AiSessionSummaryResponse response =
                aiAssistantService.createSession(
                        principal.getUserId(),
                        request);
        return ResponseEntity.created(
                        URI.create("/api/v1/ai/sessions/" + response.id()))
                .body(response);
    }

    @GetMapping("/sessions")
    @Operation(summary = "List the current user's AI chat sessions")
    public ResponseEntity<AiSessionPageResponse> listSessions(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(required = false) AiSessionMode mode,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50)
            int size,
            @RequestParam(defaultValue = "NEWEST") AiSort sort) {
        return ResponseEntity.ok(aiAssistantService.listSessions(
                principal.getUserId(),
                mode,
                page,
                size,
                sort));
    }

    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "Get an owned AI chat session")
    public ResponseEntity<AiSessionDetailResponse> getSession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(aiAssistantService.getSession(
                principal.getUserId(),
                sessionId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Soft-delete an owned AI chat session")
    public ResponseEntity<Void> deleteSession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        aiAssistantService.deleteSession(
                principal.getUserId(),
                sessionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "Send a message to an owned AI chat session")
    public ResponseEntity<AiChatResponse> sendMessage(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody SendAiMessageRequest request) {
        return ResponseEntity.ok(aiAssistantService.sendMessage(
                principal.getUserId(),
                sessionId,
                request));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "List messages in an owned AI chat session")
    public ResponseEntity<AiMessagePageResponse> listMessages(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50)
            int size) {
        return ResponseEntity.ok(aiAssistantService.listMessages(
                principal.getUserId(),
                sessionId,
                page,
                size));
    }

    @PostMapping("/explain")
    @Operation(summary = "Explain a university concept")
    public ResponseEntity<AiExplanationResponse> explainConcept(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody ExplainConceptRequest request) {
        return ResponseEntity.ok(aiAssistantService.explainConcept(
                principal.getUserId(),
                request));
    }

    @PostMapping("/summarize")
    @Operation(summary = "Generate and store a study summary")
    public ResponseEntity<AiGeneratedItemResponse> generateSummary(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody GenerateSummaryRequest request) {
        AiGeneratedItemResponse response =
                aiAssistantService.generateSummary(
                        principal.getUserId(),
                        request);
        return generatedItemCreated(response);
    }

    @PostMapping("/flashcards")
    @Operation(summary = "Generate and store study flashcards")
    public ResponseEntity<AiGeneratedItemResponse> generateFlashcards(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody GenerateFlashcardsRequest request) {
        AiGeneratedItemResponse response =
                aiAssistantService.generateFlashcards(
                        principal.getUserId(),
                        request);
        return generatedItemCreated(response);
    }

    @PostMapping("/quiz")
    @Operation(summary = "Generate and store a practice quiz")
    public ResponseEntity<AiGeneratedItemResponse> generateQuiz(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody GenerateQuizRequest request) {
        AiGeneratedItemResponse response =
                aiAssistantService.generateQuiz(
                        principal.getUserId(),
                        request);
        return generatedItemCreated(response);
    }

    @PostMapping("/study-plan")
    @Operation(summary = "Generate and store a study plan")
    public ResponseEntity<AiGeneratedItemResponse> generateStudyPlan(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @Valid @RequestBody GenerateStudyPlanRequest request) {
        AiGeneratedItemResponse response =
                aiAssistantService.generateStudyPlan(
                        principal.getUserId(),
                        request);
        return generatedItemCreated(response);
    }

    @GetMapping("/generated-items")
    @Operation(summary = "List the current user's generated study items")
    public ResponseEntity<AiGeneratedItemPageResponse>
            listGeneratedItems(
                    @AuthenticationPrincipal
                    CampusOneUserPrincipal principal,
                    @RequestParam(required = false)
                    AiGeneratedItemType itemType,
                    @RequestParam(defaultValue = "0") @Min(0) int page,
                    @RequestParam(defaultValue = "20")
                    @Min(1) @Max(50) int size) {
        return ResponseEntity.ok(
                aiAssistantService.listGeneratedItems(
                        principal.getUserId(),
                        itemType,
                        page,
                        size));
    }

    @GetMapping("/generated-items/{itemId}")
    @Operation(summary = "Get an owned generated study item")
    public ResponseEntity<AiGeneratedItemResponse> getGeneratedItem(
            @PathVariable UUID itemId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        return ResponseEntity.ok(
                aiAssistantService.getGeneratedItem(
                        principal.getUserId(),
                        itemId));
    }

    @DeleteMapping("/generated-items/{itemId}")
    @Operation(summary = "Soft-delete an owned generated study item")
    public ResponseEntity<Void> deleteGeneratedItem(
            @PathVariable UUID itemId,
            @AuthenticationPrincipal CampusOneUserPrincipal principal) {
        aiAssistantService.deleteGeneratedItem(
                principal.getUserId(),
                itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/usage")
    @Operation(summary = "List the current user's AI usage history")
    public ResponseEntity<AiUsagePageResponse> listUsage(
            @AuthenticationPrincipal CampusOneUserPrincipal principal,
            @RequestParam(required = false) AiUsageFeature feature,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50)
            int size) {
        return ResponseEntity.ok(aiAssistantService.listUsage(
                principal.getUserId(),
                feature,
                page,
                size));
    }

    private ResponseEntity<AiGeneratedItemResponse> generatedItemCreated(
            AiGeneratedItemResponse response) {
        return ResponseEntity.created(
                        URI.create(
                                "/api/v1/ai/generated-items/"
                                        + response.id()))
                .body(response);
    }
}
