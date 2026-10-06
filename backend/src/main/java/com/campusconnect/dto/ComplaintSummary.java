package com.campusconnect.dto;

import com.campusconnect.model.ComplaintStatus;
import com.campusconnect.model.Priority;
import java.time.Instant;

public record ComplaintSummary(
        Long id,
        String ticketNo,
        String title,
        String category,
        Priority priority,
        ComplaintStatus status,
        String location,
        String reporterName,
        String assigneeName,
        int upvoteCount,
        boolean upvotedByMe,
        boolean overdue,
        Instant createdAt,
        Instant dueAt
) {
}
