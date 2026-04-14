package com.bidhub.notification.domain;

import com.bidhub.notification.domain.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class NotificationTemplateTest {

    private NotificationTemplate template() {
        return NotificationTemplate.create(
                NotificationType.WELCOME,
                NotificationChannel.EMAIL,
                "Welcome {{name}}!",
                "Hello {{name}}, your account {{email}} is ready.");
    }

    @Test
    @DisplayName("render() replaces {{key}} placeholders with provided variables")
    void render_substitutesVariables() {
        NotificationTemplate t = template();
        RenderedMessage msg = t.render(Map.of("name", "Alice", "email", "alice@test.com"));
        assertThat(msg.subject()).isEqualTo("Welcome Alice!");
        assertThat(msg.body()).isEqualTo("Hello Alice, your account alice@test.com is ready.");
    }

    @Test
    @DisplayName("render() leaves unmatched placeholders as-is")
    void render_missingVariable_keepsPlaceholder() {
        NotificationTemplate t = template();
        RenderedMessage msg = t.render(Map.of("name", "Bob"));
        assertThat(msg.body()).contains("{{email}}");
    }

    @Test
    @DisplayName("render() with null vars map returns template as-is")
    void render_nullVars_returnsTemplate() {
        NotificationTemplate t = template();
        RenderedMessage msg = t.render(null);
        assertThat(msg.subject()).contains("{{name}}");
    }

    @Test
    @DisplayName("deactivate() sets isActive to false")
    void deactivate_setsInactive() {
        NotificationTemplate t = template();
        assertThat(t.isActive()).isTrue();
        t.deactivate();
        assertThat(t.isActive()).isFalse();
    }

    @Test
    @DisplayName("create() with blank subjectTemplate throws")
    void create_blankSubject_throws() {
        assertThatThrownBy(() ->
                NotificationTemplate.create(NotificationType.WELCOME,
                        NotificationChannel.EMAIL, "  ", "body"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("update() changes subject and body templates")
    void update_changesTemplates() {
        NotificationTemplate t = template();
        t.update("New Subject", "New body");
        assertThat(t.getSubjectTemplate()).isEqualTo("New Subject");
        assertThat(t.getBodyTemplate()).isEqualTo("New body");
    }
}
