package com.apaar.platform.dashboard;

import com.apaar.platform.dashboard.DashboardDtos.DashboardSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboards")
@Tag(name = "Dashboards", description = "AICTE national overview and institutional governance summary metrics")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/aicte")
    @PreAuthorize("hasAuthority('DASHBOARD_READ')")
    @Operation(summary = "Get regulatory overview dashboard for AICTE officers")
    public DashboardSummaryDto getAicteDashboard() {
        return dashboardService.getAicteDashboard();
    }

    @GetMapping("/institution")
    @PreAuthorize("hasAuthority('DASHBOARD_READ')")
    @Operation(summary = "Get institutional governance dashboard for college administrators")
    public DashboardSummaryDto getInstitutionDashboard() {
        return dashboardService.getInstitutionDashboard();
    }
}

