package com.bidhub.payment.messaging;

import com.bidhub.payment.messaging.outbox.OutboxPublisherProperties;
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

    public static final String AUCTION_EVENTS_QUEUE = "bidhub.payment.auction-events.q";
    public static final String AUCTION_EVENTS_DLQ = AUCTION_EVENTS_QUEUE + ".dlq";

    @Bean
    TopicExchange bidhubEventsExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    TopicExchange bidhubEventsDlx() {
        return ExchangeBuilder.topicExchange(DLX).durable(true).build();
    }

    @Bean
    Queue paymentAuctionEventsQueue() {
        return QueueBuilder.durable(AUCTION_EVENTS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", AUCTION_EVENTS_DLQ)
                .build();
    }

    @Bean
    Queue paymentAuctionEventsDlq() {
        return QueueBuilder.durable(AUCTION_EVENTS_DLQ).build();
    }

    @Bean
    Binding paymentBindBuyNowExecuted(Queue paymentAuctionEventsQueue, TopicExchange bidhubEventsExchange) {
        return BindingBuilder.bind(paymentAuctionEventsQueue)
                .to(bidhubEventsExchange)
                .with(RoutingKeys.AUCTION_BUY_NOW_EXECUTED);
    }

    @Bean
    Binding paymentBindAuctionSold(Queue paymentAuctionEventsQueue, TopicExchange bidhubEventsExchange) {
        return BindingBuilder.bind(paymentAuctionEventsQueue)
                .to(bidhubEventsExchange)
                .with(RoutingKeys.AUCTION_SOLD);
    }

    @Bean
    Binding paymentDlqBinding(Queue paymentAuctionEventsDlq, TopicExchange bidhubEventsDlx) {
        return BindingBuilder.bind(paymentAuctionEventsDlq)
                .to(bidhubEventsDlx)
                .with(AUCTION_EVENTS_DLQ);
    }
}
