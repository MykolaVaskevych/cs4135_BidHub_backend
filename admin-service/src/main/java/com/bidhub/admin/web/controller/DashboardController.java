package com.bidhub.admin.web.controller;

import com.bidhub.admin.application.dto.DashboardSummaryResponse;
import com.bidhub.admin.application.dto.UserSearchResponse;
import com.bidhub.admin.application.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Dashboard", description = "Admin dashboard summary and user search")
public class DashboardController {

    private final AdminDashboardService dashboardService;

    public DashboardController(AdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/admin/dashboard/summary")
    @Operation(summary = "Dashboard summary", description = "Counts of categories, reports, and moderation actions. Requires ADMIN role.")
    @ApiResponse(responseCode = "200", description = "Summary")
    public DashboardSummaryResponse getSummary(
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestHeader("X-User-Roles") String roles) {
        return dashboardService.getSummary();
    }

    @GetMapping("/api/admin/dashboard/user-search")
    @Operation(summary = "Search users", description = "Delegates to account-service. Requires ADMIN role.")
    @ApiResponse(responseCode = "200", description = "Matching users")
    public List<UserSearchResponse> searchUsers(
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestHeader("X-User-Roles") String roles,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return dashboardService.searchUsers(adminId, q, page, size);
    }
}
