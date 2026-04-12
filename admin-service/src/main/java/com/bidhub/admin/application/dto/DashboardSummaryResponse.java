package com.bidhub.admin.application.dto;

public record DashboardSummaryResponse(
        long activeCategories,
        long totalCategories,
        long pendingReports,
        long resolvedReports,
        long dismissedReports,
        long totalModerationActions) {}
