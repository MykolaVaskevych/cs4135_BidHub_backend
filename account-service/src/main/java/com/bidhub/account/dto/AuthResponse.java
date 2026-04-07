package com.bidhub.account.dto;

import com.bidhub.account.model.UserRole;
import java.util.UUID;

public record AuthResponse(
        String token, long expiresInMs, UUID userId, String email, UserRole role) {}
