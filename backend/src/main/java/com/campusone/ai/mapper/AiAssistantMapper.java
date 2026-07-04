package com.campusone.ai.mapper;

import com.campusone.ai.dto.response.AiGeneratedItemResponse;
import com.campusone.ai.dto.response.AiMessageResponse;
import com.campusone.ai.dto.response.AiSessionDetailResponse;
import com.campusone.ai.dto.response.AiSessionSummaryResponse;
import com.campusone.ai.dto.response.AiUsageRecordResponse;
import com.campusone.ai.entity.AiChatMessage;
import com.campusone.ai.entity.AiChatSession;
import com.campusone.ai.entity.AiGeneratedItem;
import com.campusone.ai.entity.AiUsageRecord;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AiAssistantMapper {

    public AiSessionSummaryResponse toSessionSummary(
            AiChatSession session) {
        return new AiSessionSummaryResponse(
                session.getId(),
                session.getTitle(),
                session.getMode(),
                session.getCreatedAt(),
                session.getUpdatedAt());
    }

    public AiSessionDetailResponse toSessionDetail(
            AiChatSession session,
            List<AiChatMessage> messages) {
        return new AiSessionDetailResponse(
                session.getId(),
                session.getTitle(),
                session.getMode(),
                messages.stream().map(this::toMessage).toList(),
                session.getCreatedAt(),
                session.getUpdatedAt());
    }

    public AiMessageResponse toMessage(AiChatMessage message) {
        return new AiMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getTokenEstimate(),
                message.getCreatedAt());
    }

    public AiGeneratedItemResponse toGeneratedItem(
            AiGeneratedItem item) {
        return new AiGeneratedItemResponse(
                item.getId(),
                item.getItemType(),
                item.getTitle(),
                item.getSourceText(),
                item.getGeneratedContent(),
                item.getCreatedAt(),
                item.getUpdatedAt());
    }

    public AiUsageRecordResponse toUsage(AiUsageRecord usage) {
        return new AiUsageRecordResponse(
                usage.getId(),
                usage.getFeature(),
                usage.getInputTokenEstimate(),
                usage.getOutputTokenEstimate(),
                usage.getProvider(),
                usage.getCreatedAt());
    }
}
