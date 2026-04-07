package com.bidhub.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 64) String firstName,
        @NotBlank @Size(max = 64) String lastName) {}
