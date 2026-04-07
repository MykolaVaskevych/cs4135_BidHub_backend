package com.bidhub.account.dto;

import com.bidhub.account.model.AccountStatus;
import com.bidhub.account.model.User;
import com.bidhub.account.model.UserRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        AccountStatus status,
        LocalDateTime createdAt,
        List<AddressResponse> addresses) {

    public static UserResponse fromEntity(User user) {
        List<AddressResponse> addresses =
                user.getAddresses().stream().map(AddressResponse::fromEntity).toList();
        return new UserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                addresses);
    }
}
