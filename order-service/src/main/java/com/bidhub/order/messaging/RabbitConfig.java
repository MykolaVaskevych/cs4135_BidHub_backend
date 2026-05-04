package com.bidhub.order.messaging;

import com.bidhub.order.messaging.outbox.OutboxPublisherProperties;
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

    public static final String PAYMENT_EVENTS_QUEUE = "bidhub.order.payment-events.q";
    public static final String PAYMENT_EVENTS_DLQ = PAYMENT_EVENTS_QUEUE + ".dlq";

    @Bean
    TopicExchange bidhubEventsExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    TopicExchange bidhubEventsDlx() {
        return ExchangeBuilder.topicExchange(DLX).durable(true).build();
    }

    @Bean
    Queue orderPaymentEventsQueue() {
        return QueueBuilder.durable(PAYMENT_EVENTS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", PAYMENT_EVENTS_DLQ)
                .build();
    }

    @Bean
    Queue orderPaymentEventsDlq() {
        return QueueBuilder.durable(PAYMENT_EVENTS_DLQ).build();
    }

    @Bean
    Binding orderBindEscrowHeld(Queue orderPaymentEventsQueue, TopicExchange bidhubEventsExchange) {
        return BindingBuilder.bind(orderPaymentEventsQueue)
                .to(bidhubEventsExchange)
                .with(RoutingKeys.PAYMENT_ESCROW_HELD);
    }

    @Bean
    Binding orderDlqBinding(Queue orderPaymentEventsDlq, TopicExchange bidhubEventsDlx) {
        return BindingBuilder.bind(orderPaymentEventsDlq)
                .to(bidhubEventsDlx)
                .with(PAYMENT_EVENTS_DLQ);
    }
}
