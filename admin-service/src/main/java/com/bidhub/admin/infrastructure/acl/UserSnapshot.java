package com.bidhub.admin.infrastructure.acl;

import java.util.UUID;

/**
 * Published Language representation of account-service's UserResponse.
 * Only the fields needed by admin-service are mapped here.
 *
 * <p>Contract: GET /api/admin/users/{userId} →
 * { userId, email, firstName, lastName, role, status: ACTIVE|SUSPENDED|BANNED|INACTIVE }
 */
public record UserSnapshot(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String role,
        String status) {}
