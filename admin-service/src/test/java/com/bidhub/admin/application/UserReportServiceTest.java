package com.bidhub.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bidhub.admin.application.dto.ReportResponse;
import com.bidhub.admin.application.dto.SubmitReportRequest;
import com.bidhub.admin.application.service.UserReportService;
import com.bidhub.admin.domain.model.ReportStatus;
import com.bidhub.admin.domain.model.UserReport;
import com.bidhub.admin.domain.repository.UserReportRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserReportServiceTest {

    @Mock private UserReportRepository userReportRepository;
    @InjectMocks private UserReportService userReportService;

    private static final UUID REPORTER_ID = UUID.randomUUID();
    private static final UUID TARGET_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();

    @Test
    @DisplayName("submitReport saves report and returns PENDING response")
    void submitReport_savesPendingReport() {
        when(userReportRepository.save(any(UserReport.class))).thenAnswer(i -> i.getArgument(0));

        SubmitReportRequest req = new SubmitReportRequest(TARGET_ID, "Spam posting");
        ReportResponse response = userReportService.submitReport(REPORTER_ID, req);

        assertThat(response.status()).isEqualTo(ReportStatus.PENDING);
        assertThat(response.reason()).isEqualTo("Spam posting");
    }

    @Test
    @DisplayName("resolveReport transitions to RESOLVED")
    void resolveReport_toResolved() {
        UserReport report = UserReport.submit(REPORTER_ID, TARGET_ID, "Spam");
        when(userReportRepository.findById(report.getReportId())).thenReturn(Optional.of(report));
        when(userReportRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ReportResponse response = userReportService.resolveReport(ADMIN_ID, report.getReportId(), "User warned");
        assertThat(response.status()).isEqualTo(ReportStatus.RESOLVED);
    }

    @Test
    @DisplayName("dismissReport transitions to DISMISSED")
    void dismissReport_toDismissed() {
        UserReport report = UserReport.submit(REPORTER_ID, TARGET_ID, "Spam");
        when(userReportRepository.findById(report.getReportId())).thenReturn(Optional.of(report));
        when(userReportRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ReportResponse response = userReportService.dismissReport(ADMIN_ID, report.getReportId(), "Not enough evidence");
        assertThat(response.status()).isEqualTo(ReportStatus.DISMISSED);
    }

    @Test
    @DisplayName("resolveReport throws when report not found")
    void resolveReport_notFound_throws() {
        UUID missing = UUID.randomUUID();
        when(userReportRepository.findById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userReportService.resolveReport(ADMIN_ID, missing, "note"))
                .isInstanceOf(RuntimeException.class);
    }
}
