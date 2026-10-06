package com.campusconnect.controller;

import com.campusconnect.dto.DashboardStats;
import com.campusconnect.model.User;
import com.campusconnect.service.DashboardService;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/dashboard")
    public DashboardStats dashboard(@AuthenticationPrincipal User user) {
        return dashboardService.stats(user);
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
