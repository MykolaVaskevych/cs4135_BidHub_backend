package com.bidhub.account.controller;

import com.bidhub.account.dto.UpdateProfileRequest;
import com.bidhub.account.dto.UserResponse;
import com.bidhub.account.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(userService.getById(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }
}
