package com.bidhub.account.controller;

import com.bidhub.account.dto.AdminActionRequest;
import com.bidhub.account.dto.UserResponse;
import com.bidhub.account.model.AccountStatus;
import com.bidhub.account.service.AdminService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> listUsers(
            @RequestParam(required = false) AccountStatus status) {
        return ResponseEntity.ok(adminService.listUsers(status));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(adminService.getUser(userId));
    }

    @PostMapping("/{userId}/suspend")
    public ResponseEntity<UserResponse> suspendUser(
            @RequestHeader("X-User-Id") UUID adminId,
            @PathVariable UUID userId,
            @Valid @RequestBody AdminActionRequest request) {
        return ResponseEntity.ok(adminService.suspendUser(adminId, userId, request));
    }

    @PostMapping("/{userId}/ban")
    public ResponseEntity<UserResponse> banUser(
            @RequestHeader("X-User-Id") UUID adminId,
            @PathVariable UUID userId,
            @Valid @RequestBody AdminActionRequest request) {
        return ResponseEntity.ok(adminService.banUser(adminId, userId, request));
    }

    @PostMapping("/{userId}/reactivate")
    public ResponseEntity<UserResponse> reactivateUser(
            @RequestHeader("X-User-Id") UUID adminId, @PathVariable UUID userId) {
        return ResponseEntity.ok(adminService.reactivateUser(adminId, userId));
    }
}
