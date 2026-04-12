package com.bidhub.admin.web.controller;

import com.bidhub.admin.application.dto.ReportResponse;
import com.bidhub.admin.application.dto.SubmitReportRequest;
import com.bidhub.admin.application.service.UserReportService;
import com.bidhub.admin.domain.model.ReportStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reports")
@Tag(name = "Reports", description = "User report management")
public class ReportController {

    private final UserReportService userReportService;

    public ReportController(UserReportService userReportService) {
        this.userReportService = userReportService;
    }

    @GetMapping
    @Operation(summary = "List reports", description = "Lists all reports. Optionally filter by status. Requires ADMIN role.")
    @ApiResponse(responseCode = "200", description = "Report list")
    public List<ReportResponse> listReports(
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestHeader("X-User-Roles") String roles,
            @RequestParam(required = false) ReportStatus status) {
        return status != null
                ? userReportService.listByStatus(status)
                : userReportService.listByStatus(null);
    }

    @GetMapping("/{reportId}")
    @Operation(summary = "Get report by ID")
    @ApiResponse(responseCode = "200", description = "Report found")
    @ApiResponse(responseCode = "404", description = "Report not found")
    public ReportResponse getReport(
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestHeader("X-User-Roles") String roles,
            @PathVariable UUID reportId) {
        return userReportService.getReport(reportId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit a report", description = "Any authenticated user can submit a report.")
    @ApiResponse(responseCode = "201", description = "Report submitted")
    @ApiResponse(responseCode = "400", description = "Validation error")
    public ReportResponse submitReport(
            @RequestHeader("X-User-Id") UUID reporterId,
            @Valid @RequestBody SubmitReportRequest req) {
        return userReportService.submitReport(reporterId, req);
    }

    @PostMapping("/{reportId}/resolve")
    @Operation(summary = "Resolve a report", description = "Requires ADMIN role.")
    @ApiResponse(responseCode = "200", description = "Report resolved")
    @ApiResponse(responseCode = "409", description = "Report is not PENDING (INV-4)")
    public ReportResponse resolveReport(
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestHeader("X-User-Roles") String roles,
            @PathVariable UUID reportId,
            @RequestParam(required = false, defaultValue = "") String note) {
        return userReportService.resolveReport(adminId, reportId, note);
    }

    @PostMapping("/{reportId}/dismiss")
    @Operation(summary = "Dismiss a report", description = "Requires ADMIN role.")
    @ApiResponse(responseCode = "200", description = "Report dismissed")
    @ApiResponse(responseCode = "409", description = "Report is not PENDING (INV-4)")
    public ReportResponse dismissReport(
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestHeader("X-User-Roles") String roles,
            @PathVariable UUID reportId,
            @RequestParam(required = false, defaultValue = "") String note) {
        return userReportService.dismissReport(adminId, reportId, note);
    }
}
