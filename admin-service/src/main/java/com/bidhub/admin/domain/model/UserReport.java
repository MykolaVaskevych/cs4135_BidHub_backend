package com.bidhub.admin.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * UserReport aggregate root. Represents a user-submitted complaint about another user.
 *
 * <p>INV-4: Status transitions are one-way: PENDING → RESOLVED or PENDING → DISMISSED.<br>
 * INV-5: A DISMISSED report cannot be re-resolved.<br>
 * INV-6: resolve() records the adminId and resolution note.
 */
@Entity
@Table(name = "user_reports")
public class UserReport {

    @Id
    @Column(name = "report_id", nullable = false, updatable = false)
    private UUID reportId;

    @Column(name = "reporter_id", nullable = false, updatable = false)
    private UUID reporterId;

    @Column(name = "target_id", nullable = false, updatable = false)
    private UUID targetId;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReportStatus status;

    @Column(name = "resolved_by_admin_id")
    private UUID resolvedByAdminId;

    @Column(name = "resolution_note")
    private String resolutionNote;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    /** JPA no-arg constructor. */
    protected UserReport() {}

    public static UserReport submit(UUID reporterId, UUID targetId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Report reason must not be blank");
        }
        UserReport report = new UserReport();
        report.reportId = UUID.randomUUID();
        report.reporterId = reporterId;
        report.targetId = targetId;
        report.reason = reason;
        report.status = ReportStatus.PENDING;
        report.submittedAt = Instant.now();
        return report;
    }

    /** Resolves the report. INV-4: only PENDING reports can be resolved. */
    public void resolve(UUID adminId, String note) {
        if (status != ReportStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING reports can be resolved; current status: " + status + " (INV-4)");
        }
        this.resolvedByAdminId = adminId;
        this.resolutionNote = note;
        this.resolvedAt = Instant.now();
        this.status = ReportStatus.RESOLVED;
    }

    /** Dismisses the report. INV-4: only PENDING reports can be dismissed. */
    public void dismiss(UUID adminId, String note) {
        if (status != ReportStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING reports can be dismissed; current status: " + status + " (INV-4)");
        }
        this.resolvedByAdminId = adminId;
        this.resolutionNote = note;
        this.resolvedAt = Instant.now();
        this.status = ReportStatus.DISMISSED;
    }

    public UUID getReportId() {
        return reportId;
    }

    public UUID getReporterId() {
        return reporterId;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public String getReason() {
        return reason;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public UUID getResolvedByAdminId() {
        return resolvedByAdminId;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
