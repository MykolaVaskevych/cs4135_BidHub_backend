package com.bidhub.notification.domain.model;

/**
 * Value object produced by NotificationTemplate.render().
 * Immutable record — subject and body are the final strings ready for dispatch.
 */
public record RenderedMessage(String subject, String body) {}
