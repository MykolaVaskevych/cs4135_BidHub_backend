package com.bidhub.delivery.messaging;

import com.bidhub.delivery.messaging.outbox.OutboxPublisherProperties;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(OutboxPublisherProperties.class)
public class RabbitConfig {

    public static final String EXCHANGE = "bidhub.events";
    public static final String DLX = "bidhub.events.dlx";

    public static final String ORDER_EVENTS_QUEUE = "bidhub.delivery.order-events.q";
    public static final String ORDER_EVENTS_DLQ = ORDER_EVENTS_QUEUE + ".dlq";

    @Bean
    TopicExchange bidhubEventsExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    TopicExchange bidhubEventsDlx() {
        return ExchangeBuilder.topicExchange(DLX).durable(true).build();
    }

    @Bean
    Queue deliveryOrderEventsQueue() {
        return QueueBuilder.durable(ORDER_EVENTS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", ORDER_EVENTS_DLQ)
                .build();
    }

    @Bean
    Queue deliveryOrderEventsDlq() {
        return QueueBuilder.durable(ORDER_EVENTS_DLQ).build();
    }

    @Bean
    Binding deliveryBindOrderCreated(Queue deliveryOrderEventsQueue, TopicExchange bidhubEventsExchange) {
        return BindingBuilder.bind(deliveryOrderEventsQueue)
                .to(bidhubEventsExchange)
                .with(RoutingKeys.ORDER_CREATED);
    }

    @Bean
    Binding deliveryDlqBinding(Queue deliveryOrderEventsDlq, TopicExchange bidhubEventsDlx) {
        return BindingBuilder.bind(deliveryOrderEventsDlq)
                .to(bidhubEventsDlx)
                .with(ORDER_EVENTS_DLQ);
    }
}
