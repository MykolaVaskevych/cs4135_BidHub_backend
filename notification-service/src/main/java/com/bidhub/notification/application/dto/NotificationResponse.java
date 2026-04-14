package com.bidhub.notification.application.dto;

import com.bidhub.notification.domain.model.Notification;
import com.bidhub.notification.domain.model.NotificationStatus;
import com.bidhub.notification.domain.model.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID notificationId,
        UUID recipientId,
        NotificationType type,
        NotificationStatus status,
        String subject,
        Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getNotificationId(),
                n.getRecipientId(),
                n.getType(),
                n.getStatus(),
                n.getSubject(),
                n.getCreatedAt()
        );
    }
}
