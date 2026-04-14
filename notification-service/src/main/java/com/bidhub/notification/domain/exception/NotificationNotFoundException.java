package com.bidhub.notification.domain.exception;

import java.util.UUID;

public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException(UUID notificationId) {
        super("Notification not found: " + notificationId);
    }
}
