package com.bidhub.notification.web.controller;

import com.bidhub.notification.application.dto.NotificationTemplateResponse;
import com.bidhub.notification.application.dto.UpdateTemplateRequest;
import com.bidhub.notification.application.service.NotificationDispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications/templates")
@Tag(name = "Notification Templates", description = "Admin management of notification templates")
public class NotificationTemplateController {

    private final NotificationDispatchService service;

    public NotificationTemplateController(NotificationDispatchService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all templates", description = "Returns all notification templates. Requires ADMIN role.")
    @ApiResponse(responseCode = "200", description = "Templates returned")
    public List<NotificationTemplateResponse> listAll() {
        return service.getAllTemplates();
    }

    @PutMapping("/{templateId}")
    @Operation(summary = "Update a template", description = "Updates subject/body template text. Requires ADMIN role.")
    @ApiResponse(responseCode = "200", description = "Template updated")
    @ApiResponse(responseCode = "404", description = "Template not found")
    public NotificationTemplateResponse update(
            @PathVariable UUID templateId,
            @RequestBody UpdateTemplateRequest req) {
        return service.updateTemplate(templateId, req);
    }
}
