package com.bidhub.notification.domain.model;

import jakarta.persistence.*;
import java.util.Map;
import java.util.UUID;

/**
 * Aggregate root representing a reusable message template.
 * Templates are rendered with variable substitution via render().
 *
 * <p>INV-N2: a matching active template must exist before dispatch proceeds.
 */
@Entity
@Table(name = "notification_templates",
       uniqueConstraints = @UniqueConstraint(columnNames = {"type", "channel"}))
public class NotificationTemplate {

    @Id
    @Column(name = "template_id", nullable = false, updatable = false)
    private UUID templateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private NotificationChannel channel;

    @Column(name = "subject_template", nullable = false, length = 512)
    private String subjectTemplate;

    @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
    private String bodyTemplate;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    protected NotificationTemplate() {}

    public static NotificationTemplate create(NotificationType type, NotificationChannel channel,
                                               String subjectTemplate, String bodyTemplate) {
        if (type == null) throw new IllegalArgumentException("type must not be null");
        if (channel == null) throw new IllegalArgumentException("channel must not be null");
        if (subjectTemplate == null || subjectTemplate.isBlank())
            throw new IllegalArgumentException("subjectTemplate must not be blank");
        if (bodyTemplate == null || bodyTemplate.isBlank())
            throw new IllegalArgumentException("bodyTemplate must not be blank");

        NotificationTemplate t = new NotificationTemplate();
        t.templateId = UUID.randomUUID();
        t.type = type;
        t.channel = channel;
        t.subjectTemplate = subjectTemplate;
        t.bodyTemplate = bodyTemplate;
        t.isActive = true;
        return t;
    }

    /**
     * Render the template by substituting {{key}} placeholders with the provided variables.
     */
    public RenderedMessage render(Map<String, String> vars) {
        String subject = interpolate(subjectTemplate, vars);
        String body = interpolate(bodyTemplate, vars);
        return new RenderedMessage(subject, body);
    }

    private String interpolate(String template, Map<String, String> vars) {
        String result = template;
        if (vars != null) {
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
        }
        return result;
    }

    public void update(String subjectTemplate, String bodyTemplate) {
        if (subjectTemplate != null && !subjectTemplate.isBlank()) this.subjectTemplate = subjectTemplate;
        if (bodyTemplate != null && !bodyTemplate.isBlank()) this.bodyTemplate = bodyTemplate;
    }

    public void deactivate() { this.isActive = false; }

    // --- Getters ---
    public UUID getTemplateId() { return templateId; }
    public NotificationType getType() { return type; }
    public NotificationChannel getChannel() { return channel; }
    public String getSubjectTemplate() { return subjectTemplate; }
    public String getBodyTemplate() { return bodyTemplate; }
    public boolean isActive() { return isActive; }
}
