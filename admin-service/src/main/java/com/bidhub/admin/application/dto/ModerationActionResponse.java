package com.bidhub.admin.application.dto;

import com.bidhub.admin.domain.model.ActionType;
import com.bidhub.admin.domain.model.ModerationAction;
import com.bidhub.admin.domain.model.ModerationTargetType;
import java.time.Instant;
import java.util.UUID;

public record ModerationActionResponse(
        UUID actionId,
        UUID adminId,
        UUID targetId,
        ModerationTargetType targetType,
        ActionType actionType,
        String reason,
        Instant createdAt) {

    public static ModerationActionResponse from(ModerationAction action) {
        return new ModerationActionResponse(
                action.getActionId(),
                action.getAdminId(),
                action.getTargetId(),
                action.getTargetType(),
                action.getActionType(),
                action.getReason(),
                action.getCreatedAt());
    }
}
