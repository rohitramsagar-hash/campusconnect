package com.campusconnect.service;

import com.campusconnect.dto.ComplaintSummary;
import com.campusconnect.dto.CountItem;
import com.campusconnect.dto.DashboardStats;
import com.campusconnect.model.ComplaintStatus;
import com.campusconnect.model.Priority;
import com.campusconnect.model.Role;
import com.campusconnect.model.User;
import com.campusconnect.repository.ComplaintRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintService complaintService;

    public DashboardService(ComplaintRepository complaintRepository, ComplaintService complaintService) {
        this.complaintRepository = complaintRepository;
        this.complaintService = complaintService;
    }

    /** Admins see campus-wide numbers, staff see their assigned work, students see their own complaints. */
    public DashboardStats stats(User actor) {
        String scope = actor.getRole() == Role.ADMIN ? "ALL" : actor.getRole() == Role.STAFF ? "ASSIGNED" : "OWN";
        Long uid = actor.getId();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ComplaintStatus s : ComplaintStatus.values()) {
            byStatus.put(s.name(), 0L);
        }
        long total = 0;
        for (Object[] row : complaintRepository.countByStatus(scope, uid)) {
            long n = (Long) row[1];
            byStatus.put(((ComplaintStatus) row[0]).name(), n);
            total += n;
        }

        List<CountItem> byCategory = complaintRepository.countByCategory(scope, uid).stream()
                .map(r -> new CountItem((String) r[0], (Long) r[1])).toList();

        Map<Priority, Long> priorityCounts = new LinkedHashMap<>();
        for (Object[] row : complaintRepository.countByPriority(scope, uid)) {
            priorityCounts.put((Priority) row[0], (Long) row[1]);
        }
        List<CountItem> byPriority = new ArrayList<>();
        for (Priority p : List.of(Priority.URGENT, Priority.HIGH, Priority.MEDIUM, Priority.LOW)) {
            byPriority.add(new CountItem(p.name(), priorityCounts.getOrDefault(p, 0L)));
        }

        long overdue = complaintRepository.countOverdue(scope, uid,
                List.of(ComplaintStatus.OPEN, ComplaintStatus.IN_PROGRESS), Instant.now());

        List<Object[]> times = complaintRepository.resolutionTimes(scope, uid);
        Double avgHours = times.isEmpty() ? null : Math.round(times.stream()
                .mapToLong(r -> Duration.between((Instant) r[0], (Instant) r[1]).toMinutes())
                .average().orElse(0) / 6.0) / 10.0;

        String listScope = switch (scope) {
            case "OWN" -> "mine";
            case "ASSIGNED" -> "assigned";
            default -> "all";
        };
        List<ComplaintSummary> recent = complaintService
                .list(actor, listScope, null, null, null, null, "newest", 0, 5).content();

        return new DashboardStats(scope, total, byStatus, overdue, avgHours, byCategory, byPriority, recent);
    }
}
