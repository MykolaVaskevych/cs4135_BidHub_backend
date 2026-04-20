package com.bidhub.auction.infrastructure.acl;

import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/** ACL: fires notification requests to notification-service. Fire-and-forget — errors are logged. */
@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final WebClient notificationWebClient;

    public NotificationClient(WebClient notificationWebClient) {
        this.notificationWebClient = notificationWebClient;
    }

    public void sendAsync(UUID recipientId, String type, Map<String, String> vars) {
        Map<String, Object> body =
                Map.of("recipientId", recipientId, "type", type, "vars", vars);
        try {
            notificationWebClient
                    .post()
                    .uri("/api/notifications/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("Notification {} sent to {}", type, recipientId);
        } catch (Exception e) {
            log.warn("Failed to send notification {} to {}: {}", type, recipientId, e.getMessage());
        }
    }
}
