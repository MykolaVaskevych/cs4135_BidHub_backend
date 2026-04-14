package com.bidhub.notification.application.service;

import com.bidhub.notification.application.dto.NotificationResponse;
import com.bidhub.notification.application.dto.NotificationTemplateResponse;
import com.bidhub.notification.application.dto.UpdateTemplateRequest;
import com.bidhub.notification.domain.exception.NotificationNotFoundException;
import com.bidhub.notification.domain.exception.NotificationTemplateNotFoundException;
import com.bidhub.notification.domain.model.*;
import com.bidhub.notification.domain.repository.NotificationRepository;
import com.bidhub.notification.domain.repository.NotificationTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class NotificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchService.class);

    private final NotificationRepository notificationRepo;
    private final NotificationTemplateRepository templateRepo;

    public NotificationDispatchService(NotificationRepository notificationRepo,
                                       NotificationTemplateRepository templateRepo) {
        this.notificationRepo = notificationRepo;
        this.templateRepo = templateRepo;
    }

    /**
     * Dispatch a notification.
     * INV-N2: active template must exist for type+channel.
     */
    public NotificationResponse dispatch(UUID recipientId, NotificationType type,
                                         NotificationChannel channel, Map<String, String> vars) {
        NotificationChannel ch = channel != null ? channel : NotificationChannel.IN_APP;

        NotificationTemplate template = templateRepo
                .findByTypeAndChannelAndIsActiveTrue(type, ch)
                .orElseThrow(() -> new NotificationTemplateNotFoundException(type, ch));

        RenderedMessage msg = template.render(vars);
        Notification notification = Notification.create(recipientId, type, ch, msg.subject(), msg.body());

        // "Send" — in dev this is a log; in prod wire to email provider / in-app store
        boolean sent = doSend(notification);
        if (sent) {
            notification.markSent(Instant.now());
        } else {
            notification.markFailed();
        }

        notificationRepo.save(notification);
        return NotificationResponse.from(notification);
    }

    private boolean doSend(Notification n) {
        // In dev, just log the notification — no actual email transport wired
        log.info("[NOTIFICATION] channel={} recipient={} type={} subject={}",
                n.getChannel(), n.getRecipientId(), n.getType(), n.getSubject());
        return true;
    }

    /**
     * Retry failed notifications — scheduled every 5 minutes.
     * INV-N3: only retries if retryCount < 3.
     */
    @Scheduled(fixedDelay = 300_000)
    public void retryFailed() {
        List<Notification> toRetry = notificationRepo.findPendingRetries(NotificationStatus.FAILED);
        for (Notification n : toRetry) {
            if (!n.canRetry()) continue;
            n.incrementRetry();
            boolean sent = doSend(n);
            if (sent) {
                n.markSent(Instant.now());
            } else {
                n.markFailed();
            }
            notificationRepo.save(n);
        }
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getForRecipient(UUID recipientId, Pageable pageable) {
        return notificationRepo.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable)
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> getAllTemplates() {
        return templateRepo.findAll().stream()
                .map(NotificationTemplateResponse::from)
                .toList();
    }

    public NotificationTemplateResponse updateTemplate(UUID templateId, UpdateTemplateRequest req) {
        NotificationTemplate t = templateRepo.findById(templateId)
                .orElseThrow(() -> new NotificationNotFoundException(templateId));
        t.update(req.subjectTemplate(), req.bodyTemplate());
        templateRepo.save(t);
        return NotificationTemplateResponse.from(t);
    }
}
