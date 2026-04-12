package com.bidhub.admin.application.dto;

import com.bidhub.admin.domain.model.ReportStatus;
import com.bidhub.admin.domain.model.UserReport;
import java.time.Instant;
import java.util.UUID;

public record ReportResponse(
        UUID reportId,
        UUID reporterId,
        UUID targetId,
        String reason,
        ReportStatus status,
        UUID resolvedByAdminId,
        String resolutionNote,
        Instant submittedAt,
        Instant resolvedAt) {

    public static ReportResponse from(UserReport report) {
        return new ReportResponse(
                report.getReportId(),
                report.getReporterId(),
                report.getTargetId(),
                report.getReason(),
                report.getStatus(),
                report.getResolvedByAdminId(),
                report.getResolutionNote(),
                report.getSubmittedAt(),
                report.getResolvedAt());
    }
}
