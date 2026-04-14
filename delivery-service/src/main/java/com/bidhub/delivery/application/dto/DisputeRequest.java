package com.bidhub.delivery.application.dto;

import jakarta.validation.constraints.NotBlank;

public record DisputeRequest(@NotBlank String reason) {}
