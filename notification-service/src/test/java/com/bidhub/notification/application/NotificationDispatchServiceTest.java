package com.bidhub.notification.application;

import com.bidhub.notification.application.dto.NotificationResponse;
import com.bidhub.notification.application.dto.UpdateTemplateRequest;
import com.bidhub.notification.application.service.NotificationDispatchService;
import com.bidhub.notification.domain.exception.NotificationTemplateNotFoundException;
import com.bidhub.notification.domain.model.*;
import com.bidhub.notification.domain.repository.NotificationRepository;
import com.bidhub.notification.domain.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

    @Mock NotificationRepository notificationRepo;
    @Mock NotificationTemplateRepository templateRepo;
    @InjectMocks NotificationDispatchService service;

    private NotificationTemplate welcomeTemplate() {
        return NotificationTemplate.create(
                NotificationType.WELCOME, NotificationChannel.IN_APP,
                "Welcome {{name}}!", "Hi {{name}}, welcome to BidHub.");
    }

    @Test
    @DisplayName("dispatch() happy path — creates notification with SENT status")
    void dispatch_happyPath_returnsSentNotification() {
        UUID recipient = UUID.randomUUID();
        NotificationTemplate t = welcomeTemplate();
        when(templateRepo.findByTypeAndChannelAndIsActiveTrue(NotificationType.WELCOME, NotificationChannel.IN_APP))
                .thenReturn(Optional.of(t));
        when(notificationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse resp = service.dispatch(recipient, NotificationType.WELCOME,
                NotificationChannel.IN_APP, Map.of("name", "Alice"));

        assertThat(resp.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(resp.recipientId()).isEqualTo(recipient);
        assertThat(resp.subject()).isEqualTo("Welcome Alice!");
    }

    @Test
    @DisplayName("INV-N2: dispatch() throws when no active template exists")
    void dispatch_noTemplate_throwsNotFound() {
        when(templateRepo.findByTypeAndChannelAndIsActiveTrue(any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.dispatch(UUID.randomUUID(), NotificationType.WELCOME,
                        NotificationChannel.IN_APP, Map.of()))
                .isInstanceOf(NotificationTemplateNotFoundException.class);

        verify(notificationRepo, never()).save(any());
    }

    @Test
    @DisplayName("dispatch() defaults to IN_APP channel when null")
    void dispatch_nullChannel_defaultsToInApp() {
        NotificationTemplate t = welcomeTemplate();
        when(templateRepo.findByTypeAndChannelAndIsActiveTrue(NotificationType.WELCOME, NotificationChannel.IN_APP))
                .thenReturn(Optional.of(t));
        when(notificationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse resp = service.dispatch(UUID.randomUUID(), NotificationType.WELCOME,
                null, Map.of("name", "Bob"));

        assertThat(resp).isNotNull();
    }

    @Test
    @DisplayName("updateTemplate() updates subjectTemplate and bodyTemplate")
    void updateTemplate_updatesFields() {
        NotificationTemplate t = welcomeTemplate();
        when(templateRepo.findById(t.getTemplateId())).thenReturn(Optional.of(t));
        when(templateRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.updateTemplate(t.getTemplateId(),
                new UpdateTemplateRequest("New Subject", "New body"));

        assertThat(resp.subjectTemplate()).isEqualTo("New Subject");
        assertThat(resp.bodyTemplate()).isEqualTo("New body");
    }
}
