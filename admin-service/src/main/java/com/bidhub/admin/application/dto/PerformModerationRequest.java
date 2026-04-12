package com.bidhub.admin.application.dto;

import com.bidhub.admin.domain.model.ActionType;
import com.bidhub.admin.domain.model.ModerationTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PerformModerationRequest(
        @NotNull UUID targetId,
        @NotNull ModerationTargetType targetType,
        @NotNull ActionType actionType,
        @NotBlank String reason) {}
