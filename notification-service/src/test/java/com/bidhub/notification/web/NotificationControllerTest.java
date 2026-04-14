package com.bidhub.notification.web;

import com.bidhub.notification.application.dto.NotificationResponse;
import com.bidhub.notification.application.dto.SendNotificationRequest;
import com.bidhub.notification.application.service.NotificationDispatchService;
import com.bidhub.notification.domain.exception.NotificationTemplateNotFoundException;
import com.bidhub.notification.domain.model.NotificationChannel;
import com.bidhub.notification.domain.model.NotificationStatus;
import com.bidhub.notification.domain.model.NotificationType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = com.bidhub.notification.web.controller.NotificationController.class)
class NotificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private NotificationDispatchService service;

    @Test
    @DisplayName("POST /api/notifications/send → 201 Created")
    void send_returns201() throws Exception {
        UUID recipientId = UUID.randomUUID();
        NotificationResponse resp = new NotificationResponse(
                UUID.randomUUID(), recipientId, NotificationType.WELCOME,
                NotificationStatus.SENT, "Welcome Alice!", Instant.now());

        when(service.dispatch(any(), any(), any(), any())).thenReturn(resp);

        SendNotificationRequest req = new SendNotificationRequest(
                recipientId, NotificationType.WELCOME, NotificationChannel.IN_APP,
                Map.of("name", "Alice"));

        mockMvc.perform(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.subject").value("Welcome Alice!"));
    }

    @Test
    @DisplayName("POST /api/notifications/send → 404 when no active template")
    void send_noTemplate_returns404() throws Exception {
        UUID recipientId = UUID.randomUUID();
        when(service.dispatch(any(), any(), any(), any()))
                .thenThrow(new NotificationTemplateNotFoundException(NotificationType.WELCOME, NotificationChannel.IN_APP));

        SendNotificationRequest req = new SendNotificationRequest(
                recipientId, NotificationType.WELCOME, NotificationChannel.IN_APP, Map.of());

        mockMvc.perform(post("/api/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }
}
