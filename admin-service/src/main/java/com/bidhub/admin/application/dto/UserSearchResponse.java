package com.bidhub.admin.application.dto;

import java.util.UUID;

public record UserSearchResponse(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String role,
        String status) {}
