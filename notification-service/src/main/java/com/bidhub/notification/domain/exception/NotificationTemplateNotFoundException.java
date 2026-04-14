package com.bidhub.notification.domain.exception;

import com.bidhub.notification.domain.model.NotificationChannel;
import com.bidhub.notification.domain.model.NotificationType;

public class NotificationTemplateNotFoundException extends RuntimeException {
    public NotificationTemplateNotFoundException(NotificationType type, NotificationChannel channel) {
        super("No active notification template found for type=" + type + " channel=" + channel);
    }
}
