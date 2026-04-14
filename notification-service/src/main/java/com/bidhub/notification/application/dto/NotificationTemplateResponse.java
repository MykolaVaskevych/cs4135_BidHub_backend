package com.bidhub.notification.application.dto;

import com.bidhub.notification.domain.model.NotificationChannel;
import com.bidhub.notification.domain.model.NotificationTemplate;
import com.bidhub.notification.domain.model.NotificationType;

import java.util.UUID;

public record NotificationTemplateResponse(
        UUID templateId,
        NotificationType type,
        NotificationChannel channel,
        String subjectTemplate,
        String bodyTemplate,
        boolean isActive
) {
    public static NotificationTemplateResponse from(NotificationTemplate t) {
        return new NotificationTemplateResponse(
                t.getTemplateId(), t.getType(), t.getChannel(),
                t.getSubjectTemplate(), t.getBodyTemplate(), t.isActive());
    }
}
