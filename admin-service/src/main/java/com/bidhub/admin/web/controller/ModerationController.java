package com.bidhub.admin.web.controller;

import com.bidhub.admin.application.dto.ModerationActionResponse;
import com.bidhub.admin.application.dto.PerformModerationRequest;
import com.bidhub.admin.application.service.ModerationActionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/moderation-actions")
@Tag(name = "Moderation Actions", description = "Admin moderation action management (INV-7/8)")
public class ModerationController {

    private final ModerationActionService moderationActionService;

    public ModerationController(ModerationActionService moderationActionService) {
        this.moderationActionService = moderationActionService;
    }

    @GetMapping
    @Operation(summary = "List moderation actions", description = "Filter by adminId or targetId. Requires ADMIN role.")
    @ApiResponse(responseCode = "200", description = "Action list")
    public List<ModerationActionResponse> listActions(
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestHeader("X-User-Roles") String roles,
            @RequestParam(required = false) UUID filterAdminId,
            @RequestParam(required = false) UUID targetId) {
        return moderationActionService.listActions(filterAdminId, targetId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Perform moderation action", description = "Creates an append-only moderation record. Requires ADMIN role.")
    @ApiResponse(responseCode = "201", description = "Action recorded")
    @ApiResponse(responseCode = "400", description = "Validation error")
    public ModerationActionResponse performAction(
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestHeader("X-User-Roles") String roles,
            @Valid @RequestBody PerformModerationRequest req) {
        return moderationActionService.performAction(adminId, req);
    }
}
