package com.bidhub.admin.application.service;

import com.bidhub.admin.application.dto.DashboardSummaryResponse;
import com.bidhub.admin.application.dto.UserSearchResponse;
import com.bidhub.admin.domain.model.ReportStatus;
import com.bidhub.admin.domain.repository.CategoryRepository;
import com.bidhub.admin.domain.repository.ModerationActionRepository;
import com.bidhub.admin.domain.repository.UserReportRepository;
import com.bidhub.admin.infrastructure.acl.AccountClient;
import com.bidhub.admin.infrastructure.acl.UserSnapshot;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final CategoryRepository categoryRepository;
    private final UserReportRepository userReportRepository;
    private final ModerationActionRepository moderationActionRepository;
    private final AccountClient accountClient;

    public AdminDashboardService(
            CategoryRepository categoryRepository,
            UserReportRepository userReportRepository,
            ModerationActionRepository moderationActionRepository,
            AccountClient accountClient) {
        this.categoryRepository = categoryRepository;
        this.userReportRepository = userReportRepository;
        this.moderationActionRepository = moderationActionRepository;
        this.accountClient = accountClient;
    }

    public DashboardSummaryResponse getSummary() {
        long activeCategories = categoryRepository.findByIsActiveTrue().size();
        long totalCategories = categoryRepository.count();
        long pendingReports = userReportRepository.findByStatus(ReportStatus.PENDING).size();
        long resolvedReports = userReportRepository.findByStatus(ReportStatus.RESOLVED).size();
        long dismissedReports = userReportRepository.findByStatus(ReportStatus.DISMISSED).size();
        long totalModerationActions = moderationActionRepository.count();
        return new DashboardSummaryResponse(
                activeCategories,
                totalCategories,
                pendingReports,
                resolvedReports,
                dismissedReports,
                totalModerationActions);
    }

    public List<UserSearchResponse> searchUsers(UUID adminId, String keyword, int page, int size) {
        List<UserSnapshot> snapshots = accountClient.searchUsers(adminId, keyword, page, size);
        return snapshots.stream()
                .map(s -> new UserSearchResponse(
                        s.userId(), s.email(), s.firstName(), s.lastName(), s.role(), s.status()))
                .toList();
    }
}
