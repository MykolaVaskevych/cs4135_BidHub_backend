package com.bidhub.notification.application.dto;

import com.bidhub.notification.domain.model.NotificationChannel;
import com.bidhub.notification.domain.model.NotificationType;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record SendNotificationRequest(
        @NotNull UUID recipientId,
        @NotNull NotificationType type,
        NotificationChannel channel,
        Map<String, String> vars
) {
    public SendNotificationRequest {
        if (channel == null) channel = NotificationChannel.IN_APP;
        if (vars == null) vars = Map.of();
    }
}
