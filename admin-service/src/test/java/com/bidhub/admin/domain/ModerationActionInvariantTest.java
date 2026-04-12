package com.bidhub.admin.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bidhub.admin.domain.model.ActionType;
import com.bidhub.admin.domain.model.ModerationAction;
import com.bidhub.admin.domain.model.ModerationTargetType;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ModerationActionInvariantTest {

    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID TARGET_ID = UUID.randomUUID();

    @Test
    @DisplayName("INV-8: ModerationAction.create records adminId and actionType")
    void create_recordsAdminAndType() {
        ModerationAction action =
                ModerationAction.create(
                        ADMIN_ID, TARGET_ID, ModerationTargetType.USER, ActionType.WARN, "Spam warning");
        assertThat(action.getAdminId()).isEqualTo(ADMIN_ID);
        assertThat(action.getActionType()).isEqualTo(ActionType.WARN);
        assertThat(action.getTargetType()).isEqualTo(ModerationTargetType.USER);
        assertThat(action.getReason()).isEqualTo("Spam warning");
    }

    @Test
    @DisplayName("INV-8: adminId must not be null")
    void create_nullAdminId_throws() {
        assertThatThrownBy(
                        () ->
                                ModerationAction.create(
                                        null,
                                        TARGET_ID,
                                        ModerationTargetType.USER,
                                        ActionType.BAN,
                                        "reason"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("INV-7: ModerationAction is append-only — no setters exposed")
    void appendOnly_noPublicSetters() throws Exception {
        // Verify via reflection that no public setter methods exist on ModerationAction
        long setterCount =
                java.util.Arrays.stream(ModerationAction.class.getMethods())
                        .filter(m -> m.getName().startsWith("set"))
                        .count();
        assertThat(setterCount)
                .as("ModerationAction must have no public setters (INV-7 append-only)")
                .isZero();
    }

    @Test
    @DisplayName("INV-7: ModerationAction createdAt is set at construction time")
    void create_setsCreatedAt() {
        ModerationAction action =
                ModerationAction.create(
                        ADMIN_ID, TARGET_ID, ModerationTargetType.USER, ActionType.SUSPEND, "Spam");
        assertThat(action.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("INV-8: reason must not be blank")
    void create_blankReason_throws() {
        assertThatThrownBy(
                        () ->
                                ModerationAction.create(
                                        ADMIN_ID,
                                        TARGET_ID,
                                        ModerationTargetType.USER,
                                        ActionType.WARN,
                                        ""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
