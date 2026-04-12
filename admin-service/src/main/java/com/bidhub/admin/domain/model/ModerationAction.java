package com.bidhub.admin.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * ModerationAction aggregate root. Append-only audit record of admin moderation decisions.
 *
 * <p>INV-7: Append-only — no update methods, no public setters.<br>
 * INV-8: adminId required; reason must not be blank.
 */
@Entity
@Table(name = "moderation_actions")
public class ModerationAction {

    @Id
    @Column(name = "action_id", nullable = false, updatable = false)
    private UUID actionId;

    @Column(name = "admin_id", nullable = false, updatable = false)
    private UUID adminId;

    @Column(name = "target_id", nullable = false, updatable = false)
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, updatable = false)
    private ModerationTargetType targetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, updatable = false)
    private ActionType actionType;

    @Column(name = "reason", nullable = false, updatable = false)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** JPA no-arg constructor. */
    protected ModerationAction() {}

    public static ModerationAction create(
            UUID adminId,
            UUID targetId,
            ModerationTargetType targetType,
            ActionType actionType,
            String reason) {
        if (adminId == null) {
            throw new IllegalArgumentException("adminId must not be null (INV-8)");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank (INV-8)");
        }
        ModerationAction action = new ModerationAction();
        action.actionId = UUID.randomUUID();
        action.adminId = Objects.requireNonNull(adminId);
        action.targetId = Objects.requireNonNull(targetId);
        action.targetType = Objects.requireNonNull(targetType);
        action.actionType = Objects.requireNonNull(actionType);
        action.reason = reason;
        action.createdAt = Instant.now();
        return action;
    }

    public UUID getActionId() {
        return actionId;
    }

    public UUID getAdminId() {
        return adminId;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public ModerationTargetType getTargetType() {
        return targetType;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
