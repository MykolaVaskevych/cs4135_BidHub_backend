package com.bidhub.notification.domain.repository;

import com.bidhub.notification.domain.model.Notification;
import com.bidhub.notification.domain.model.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

/**
 * INV-N4: No deleteById exposed — Notification is append-only.
 */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.status = :status AND n.retryCount < 3")
    List<Notification> findPendingRetries(NotificationStatus status);
}
