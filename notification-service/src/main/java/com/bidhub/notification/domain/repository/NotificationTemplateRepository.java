package com.bidhub.notification.domain.repository;

import com.bidhub.notification.domain.model.NotificationChannel;
import com.bidhub.notification.domain.model.NotificationTemplate;
import com.bidhub.notification.domain.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    Optional<NotificationTemplate> findByTypeAndChannelAndIsActiveTrue(
            NotificationType type, NotificationChannel channel);
}
