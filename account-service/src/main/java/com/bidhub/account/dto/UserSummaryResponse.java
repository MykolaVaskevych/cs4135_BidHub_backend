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
        LocalDateTime createdAt) {

    public static UserSummaryResponse fromEntity(User user) {
        return new UserSummaryResponse(
                user.getUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt());
    }
}
