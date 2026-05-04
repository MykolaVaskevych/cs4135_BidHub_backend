package com.bidhub.order.messaging.outbox;

import com.bidhub.order.messaging.RabbitConfig;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        value = "bidhub.outbox.publisher.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventStateService stateService;
    private final RabbitTemplate rabbitTemplate;

    public OutboxPublisher(OutboxEventStateService stateService, RabbitTemplate rabbitTemplate) {
        this.stateService = stateService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelayString = "${bidhub.outbox.publisher.poll-ms:1000}")
    public void publishPending() {
        List<OutboxEventStateService.ClaimedEvent> claimed = stateService.claimBatch();
        for (OutboxEventStateService.ClaimedEvent c : claimed) {
            try {
                Message msg = buildJsonMessage(c.envelopeJson());
                rabbitTemplate.send(RabbitConfig.EXCHANGE, c.routingKey(), msg);
                stateService.markPublished(c.id());
            } catch (Exception ex) {
                log.warn("Publish failed for outbox id={} routingKey={}", c.id(), c.routingKey(), ex);
                stateService.handleFailure(c.id(), ex);
            }
        }
    }

    private Message buildJsonMessage(String envelopeJson) {
        MessageProperties props = new MessageProperties();
        props.setContentType("application/json");
        props.setContentEncoding(StandardCharsets.UTF_8.name());
        return new Message(envelopeJson.getBytes(StandardCharsets.UTF_8), props);
    }
}
