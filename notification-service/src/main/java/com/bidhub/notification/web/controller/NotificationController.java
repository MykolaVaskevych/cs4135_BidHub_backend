package com.bidhub.notification.web.controller;

import com.bidhub.notification.application.dto.NotificationResponse;
import com.bidhub.notification.application.dto.SendNotificationRequest;
import com.bidhub.notification.application.service.NotificationDispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Notification dispatch and inbox management")
public class NotificationController {

    private final NotificationDispatchService service;

    public NotificationController(NotificationDispatchService service) {
        this.service = service;
    }

    @PostMapping("/send")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Send a notification", description = "Dispatches a notification to a recipient. Called by other services.")
    @ApiResponse(responseCode = "201", description = "Notification dispatched")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "No active template found for type+channel")
    public NotificationResponse send(@Valid @RequestBody SendNotificationRequest req) {
        return service.dispatch(req.recipientId(), req.type(), req.channel(), req.vars());
    }

    @GetMapping("/me")
    @Operation(summary = "Get my notifications", description = "Returns paginated list of notifications for the authenticated user.")
    @ApiResponse(responseCode = "200", description = "Notifications returned")
    public Page<NotificationResponse> myNotifications(
            @RequestHeader("X-User-Id") UUID userId,
            Pageable pageable) {
        return service.getForRecipient(userId, pageable);
    }
}
