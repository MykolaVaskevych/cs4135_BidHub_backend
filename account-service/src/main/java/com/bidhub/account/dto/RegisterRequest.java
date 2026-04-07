package com.bidhub.account.dto;

import com.bidhub.account.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotBlank @Size(max = 64) String firstName,
        @NotBlank @Size(max = 64) String lastName,
        UserRole role) {}
