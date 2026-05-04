package com.bidhub.auction.messaging;

import com.bidhub.auction.messaging.outbox.OutboxPublisherProperties;
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

    public static final String PAYMENT_EVENTS_QUEUE = "bidhub.auction.payment-events.q";
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
    Queue auctionPaymentEventsQueue() {
        return QueueBuilder.durable(PAYMENT_EVENTS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", PAYMENT_EVENTS_DLQ)
                .build();
    }

    @Bean
    Queue auctionPaymentEventsDlq() {
        return QueueBuilder.durable(PAYMENT_EVENTS_DLQ).build();
    }

    @Bean
    Binding auctionBindEscrowHeld(Queue auctionPaymentEventsQueue, TopicExchange bidhubEventsExchange) {
        return BindingBuilder.bind(auctionPaymentEventsQueue)
                .to(bidhubEventsExchange)
                .with(RoutingKeys.PAYMENT_ESCROW_HELD);
    }

    @Bean
    Binding auctionBindPaymentFailed(Queue auctionPaymentEventsQueue, TopicExchange bidhubEventsExchange) {
        return BindingBuilder.bind(auctionPaymentEventsQueue)
                .to(bidhubEventsExchange)
                .with(RoutingKeys.PAYMENT_FAILED);
    }

    @Bean
    Binding auctionDlqBinding(Queue auctionPaymentEventsDlq, TopicExchange bidhubEventsDlx) {
        return BindingBuilder.bind(auctionPaymentEventsDlq)
                .to(bidhubEventsDlx)
                .with(PAYMENT_EVENTS_DLQ);
    }
}
