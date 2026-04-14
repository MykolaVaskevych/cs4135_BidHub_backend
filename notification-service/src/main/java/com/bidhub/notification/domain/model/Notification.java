package com.bidhub.notification.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root representing a single dispatched notification.
 *
 * <p>Invariants:
 * INV-N1: recipientId must not be null (enforced at factory).
 * INV-N3: retry capped at 3; canRetry() returns false after 3 failures.
 * INV-N4: append-only — @PreUpdate guard prevents updates after creation.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @Column(name = "notification_id", nullable = false, updatable = false)
    private UUID notificationId;

    @Column(name = "recipient_id", nullable = false, updatable = false)
    private UUID recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private NotificationChannel channel;

    @Column(nullable = false, updatable = false, length = 512)
    private String subject;

    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(nullable = false)
    private int retryCount;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {}

    /**
     * Factory method — enforces INV-N1 (recipientId required).
     */
    public static Notification create(UUID recipientId, NotificationType type,
                                      NotificationChannel channel, String subject, String body) {
        if (recipientId == null) throw new IllegalArgumentException("recipientId must not be null (INV-N1)");
        if (type == null) throw new IllegalArgumentException("type must not be null");
        if (channel == null) throw new IllegalArgumentException("channel must not be null");

        Notification n = new Notification();
        n.notificationId = UUID.randomUUID();
        n.recipientId = recipientId;
        n.type = type;
        n.channel = channel;
        n.subject = subject;
        n.body = body;
        n.status = NotificationStatus.PENDING;
        n.retryCount = 0;
        n.createdAt = Instant.now();
        return n;
    }

    /** INV-N3: canRetry() — false after 3 failures. */
    public boolean canRetry() {
        return retryCount < 3;
    }

    public void markSent(Instant at) {
        this.status = NotificationStatus.SENT;
        this.sentAt = at;
    }

    public void markFailed() {
        this.status = NotificationStatus.FAILED;
    }

    public void incrementRetry() {
        this.retryCount++;
        this.status = NotificationStatus.PENDING;
    }

    /**
     * INV-N4: append-only guard — prevent any JPA update after initial persist.
     */
    @PreUpdate
    void guardAppendOnly() {
        throw new UnsupportedOperationException(
                "Notification is append-only (INV-N4). Status transitions must go through command methods; " +
                "the entity itself must not be updated via JPA merge after markSent/markFailed.");
    }

    // --- Getters ---
    public UUID getNotificationId() { return notificationId; }
    public UUID getRecipientId() { return recipientId; }
    public NotificationType getType() { return type; }
    public NotificationChannel getChannel() { return channel; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public NotificationStatus getStatus() { return status; }
    public int getRetryCount() { return retryCount; }
    public Instant getSentAt() { return sentAt; }
    public Instant getCreatedAt() { return createdAt; }
}
