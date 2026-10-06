package com.campusconnect.dto;

import java.util.List;
import java.util.Map;

public record DashboardStats(
        String scope,
        long total,
        Map<String, Long> byStatus,
        long overdue,
        Double avgResolutionHours,
        List<CountItem> byCategory,
        List<CountItem> byPriority,
        List<ComplaintSummary> recent
) {
}
