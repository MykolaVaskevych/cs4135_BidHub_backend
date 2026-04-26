package com.bidhub.account.dto;

import com.bidhub.account.model.AccountStatus;
import com.bidhub.account.model.User;
import com.bidhub.account.model.UserRole;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserSummaryResponse(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        AccountStatus status,
        LocalDateTime createdAt,
        LocalDateTime suspendedAt,
        String suspensionReason,
        LocalDateTime bannedAt,
        String banReason) {

    public static UserSummaryResponse fromEntity(User user) {
        return new UserSummaryResponse(
                user.getUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getSuspendedAt(),
                user.getSuspensionReason(),
                user.getBannedAt(),
                user.getBanReason());
    }
}
