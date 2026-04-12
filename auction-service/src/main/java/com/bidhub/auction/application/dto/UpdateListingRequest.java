package com.bidhub.auction.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record UpdateListingRequest(
        @NotBlank String title,
        @NotBlank String description,
        @NotEmpty List<String> photos,
        @NotNull UUID categoryId) {}
