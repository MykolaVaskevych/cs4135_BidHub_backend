package com.bidhub.admin.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SubmitReportRequest(@NotNull UUID targetId, @NotBlank String reason) {}
