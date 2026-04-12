package com.bidhub.admin.domain.repository;

import com.bidhub.admin.domain.model.ReportStatus;
import com.bidhub.admin.domain.model.UserReport;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserReportRepository extends JpaRepository<UserReport, UUID> {

    List<UserReport> findByStatus(ReportStatus status);

    List<UserReport> findByTargetId(UUID targetId);
}
