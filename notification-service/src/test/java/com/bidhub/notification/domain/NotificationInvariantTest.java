package com.bidhub.notification.domain;

import com.bidhub.notification.domain.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class NotificationInvariantTest {

    private static final UUID RECIPIENT = UUID.randomUUID();

    private Notification notification() {
        return Notification.create(RECIPIENT, NotificationType.WELCOME,
                NotificationChannel.EMAIL, "Welcome to BidHub", "Hello!");
    }

    @Test
    @DisplayName("INV-N1: recipientId must not be null")
    void create_nullRecipient_throws() {
        assertThatThrownBy(() ->
                Notification.create(null, NotificationType.WELCOME,
                        NotificationChannel.EMAIL, "subj", "body"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INV-N1");
    }

    @Test
    @DisplayName("INV-N1: valid recipientId creates notification in PENDING status")
    void create_validRecipient_isPending() {
        Notification n = notification();
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(n.getRecipientId()).isEqualTo(RECIPIENT);
        assertThat(n.getRetryCount()).isZero();
    }

    @Test
    @DisplayName("markSent transitions status to SENT and records sentAt")
    void markSent_setsSentStatusAndTime() {
        Notification n = notification();
        Instant now = Instant.now();
        n.markSent(now);
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(n.getSentAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("markFailed transitions status to FAILED")
    void markFailed_setsFailedStatus() {
        Notification n = notification();
        n.markFailed();
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.FAILED);
    }

    @Test
    @DisplayName("INV-N3: canRetry is true for retryCount < 3")
    void canRetry_belowLimit_returnsTrue() {
        Notification n = notification();
        n.incrementRetry();
        n.incrementRetry();
        assertThat(n.canRetry()).isTrue();
        assertThat(n.getRetryCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("INV-N3: canRetry is false after 3 retries")
    void canRetry_atLimit_returnsFalse() {
        Notification n = notification();
        n.incrementRetry();
        n.incrementRetry();
        n.incrementRetry();
        assertThat(n.canRetry()).isFalse();
        assertThat(n.getRetryCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("notificationId and createdAt are set on creation")
    void create_setsIdAndTimestamp() {
        Notification n = notification();
        assertThat(n.getNotificationId()).isNotNull();
        assertThat(n.getCreatedAt()).isNotNull();
    }
}
