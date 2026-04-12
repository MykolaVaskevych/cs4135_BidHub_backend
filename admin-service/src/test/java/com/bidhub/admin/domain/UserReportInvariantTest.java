package com.bidhub.admin.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bidhub.admin.domain.model.ReportStatus;
import com.bidhub.admin.domain.model.UserReport;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserReportInvariantTest {

    private static final UUID REPORTER_ID = UUID.randomUUID();
    private static final UUID TARGET_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();

    @Test
    @DisplayName("INV-4: New report starts in PENDING status")
    void submit_startsPending() {
        UserReport report = UserReport.submit(REPORTER_ID, TARGET_ID, "Spam");
        assertThat(report.getStatus()).isEqualTo(ReportStatus.PENDING);
    }

    @Test
    @DisplayName("INV-4: PENDING report can be resolved → RESOLVED")
    void resolve_pendingReport_succeeds() {
        UserReport report = UserReport.submit(REPORTER_ID, TARGET_ID, "Spam");
        report.resolve(ADMIN_ID, "Actioned");
        assertThat(report.getStatus()).isEqualTo(ReportStatus.RESOLVED);
    }

    @Test
    @DisplayName("INV-4: PENDING report can be dismissed → DISMISSED")
    void dismiss_pendingReport_succeeds() {
        UserReport report = UserReport.submit(REPORTER_ID, TARGET_ID, "Spam");
        report.dismiss(ADMIN_ID, "Not enough evidence");
        assertThat(report.getStatus()).isEqualTo(ReportStatus.DISMISSED);
    }

    @Test
    @DisplayName("INV-4: RESOLVED report cannot be resolved again (one-way state machine)")
    void resolve_resolvedReport_throws() {
        UserReport report = UserReport.submit(REPORTER_ID, TARGET_ID, "Spam");
        report.resolve(ADMIN_ID, "Done");
        assertThatThrownBy(() -> report.resolve(ADMIN_ID, "Again"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("INV-5: DISMISSED report cannot be resolved")
    void resolve_dismissedReport_throws() {
        UserReport report = UserReport.submit(REPORTER_ID, TARGET_ID, "Spam");
        report.dismiss(ADMIN_ID, "No evidence");
        assertThatThrownBy(() -> report.resolve(ADMIN_ID, "Changed mind"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("INV-6: Resolve records adminId and resolution note")
    void resolve_recordsAdminAndNote() {
        UserReport report = UserReport.submit(REPORTER_ID, TARGET_ID, "Spam");
        report.resolve(ADMIN_ID, "User warned");
        assertThat(report.getResolvedByAdminId()).isEqualTo(ADMIN_ID);
        assertThat(report.getResolutionNote()).isEqualTo("User warned");
    }

    @Test
    @DisplayName("INV-4: reason must not be blank on submit")
    void submit_blankReason_throws() {
        assertThatThrownBy(() -> UserReport.submit(REPORTER_ID, TARGET_ID, ""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
