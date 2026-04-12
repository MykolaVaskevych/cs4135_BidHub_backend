package com.bidhub.admin.application.service;

import com.bidhub.admin.application.dto.ReportResponse;
import com.bidhub.admin.application.dto.SubmitReportRequest;
import com.bidhub.admin.domain.model.ReportStatus;
import com.bidhub.admin.domain.model.UserReport;
import com.bidhub.admin.domain.repository.UserReportRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserReportService {

    private final UserReportRepository userReportRepository;

    public UserReportService(UserReportRepository userReportRepository) {
        this.userReportRepository = userReportRepository;
    }

    public ReportResponse submitReport(UUID reporterId, SubmitReportRequest req) {
        UserReport report = UserReport.submit(reporterId, req.targetId(), req.reason());
        return ReportResponse.from(userReportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public ReportResponse getReport(UUID reportId) {
        return userReportRepository
                .findById(reportId)
                .map(ReportResponse::from)
                .orElseThrow(() -> new NoSuchElementException("Report not found: " + reportId));
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> listByStatus(ReportStatus status) {
        return userReportRepository.findByStatus(status).stream()
                .map(ReportResponse::from)
                .toList();
    }

    public ReportResponse resolveReport(UUID adminId, UUID reportId, String note) {
        UserReport report =
                userReportRepository
                        .findById(reportId)
                        .orElseThrow(() -> new NoSuchElementException("Report not found: " + reportId));
        report.resolve(adminId, note);
        return ReportResponse.from(userReportRepository.save(report));
    }

    public ReportResponse dismissReport(UUID adminId, UUID reportId, String note) {
        UserReport report =
                userReportRepository
                        .findById(reportId)
                        .orElseThrow(() -> new NoSuchElementException("Report not found: " + reportId));
        report.dismiss(adminId, note);
        return ReportResponse.from(userReportRepository.save(report));
    }
}
