package com.campusconnect.dto;

import com.campusconnect.model.ComplaintStatus;
import com.campusconnect.model.Priority;
import java.time.Instant;
import java.util.List;

public record ComplaintDetail(
        Long id,
        String ticketNo,
        String title,
        String description,
        Long categoryId,
        String category,
        Priority priority,
        ComplaintStatus status,
        String location,
        boolean anonymous,
        boolean isPublic,
        String reporterName,
        PersonRef assignee,
        int upvoteCount,
        boolean upvotedByMe,
        boolean overdue,
        Instant createdAt,
        Instant updatedAt,
        Instant dueAt,
        Instant resolvedAt,
        List<CommentDto> comments,
        List<HistoryDto> history,
        Permissions permissions
) {
}
