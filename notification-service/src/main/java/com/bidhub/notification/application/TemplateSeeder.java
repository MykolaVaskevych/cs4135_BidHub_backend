package com.bidhub.notification.application;

import com.bidhub.notification.domain.model.NotificationChannel;
import com.bidhub.notification.domain.model.NotificationTemplate;
import com.bidhub.notification.domain.model.NotificationType;
import com.bidhub.notification.domain.repository.NotificationTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class TemplateSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TemplateSeeder.class);

    private final NotificationTemplateRepository repo;

    public TemplateSeeder(NotificationTemplateRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed(NotificationType.BID_OUTBID, NotificationChannel.IN_APP,
                "You have been outbid",
                "Your bid on \"{{title}}\" was outbid. New highest bid: €{{amount}}.");

        seed(NotificationType.AUCTION_WON, NotificationChannel.IN_APP,
                "You won the auction!",
                "Congratulations! You won \"{{title}}\" with a bid of €{{amount}}.");

        seed(NotificationType.AUCTION_ENDED_SELLER, NotificationChannel.IN_APP,
                "Your auction has ended",
                "Your auction \"{{title}}\" has ended. Status: {{status}}.");

        seed(NotificationType.WELCOME, NotificationChannel.IN_APP,
                "Welcome to BidHub!",
                "Hi {{firstName}}, welcome to BidHub. Start exploring auctions!");

        seed(NotificationType.PAYMENT_RECEIPT, NotificationChannel.IN_APP,
                "Payment received",
                "Payment of €{{amount}} received. Transaction: {{transactionId}}.");

        seed(NotificationType.ORDER_CREATED, NotificationChannel.IN_APP,
                "Order created",
                "Your order {{orderId}} has been created and is being processed.");

        seed(NotificationType.ORDER_COMPLETED, NotificationChannel.IN_APP,
                "Order completed",
                "Your order {{orderId}} has been completed.");

        seed(NotificationType.ORDER_CANCELLED, NotificationChannel.IN_APP,
                "Order cancelled",
                "Your order {{orderId}} has been cancelled.");

        seed(NotificationType.DRIVER_ASSIGNED, NotificationChannel.IN_APP,
                "Driver assigned",
                "A driver has been assigned to your delivery for order {{orderId}}.");

        seed(NotificationType.GOODS_DELIVERED, NotificationChannel.IN_APP,
                "Goods delivered",
                "Your order {{orderId}} has been delivered.");

        seed(NotificationType.DISPUTE_RAISED, NotificationChannel.IN_APP,
                "Dispute raised",
                "A dispute has been raised on order {{orderId}}: {{reason}}.");

        seed(NotificationType.USER_SUSPENDED, NotificationChannel.IN_APP,
                "Account suspended",
                "Your account has been suspended. Reason: {{reason}}.");

        log.info("Notification templates seeded.");
    }

    private void seed(NotificationType type, NotificationChannel channel,
                      String subject, String body) {
        if (repo.findByTypeAndChannelAndIsActiveTrue(type, channel).isEmpty()) {
            repo.save(NotificationTemplate.create(type, channel, subject, body));
        }
    }
}
