package com.campusconnect.dto;

import com.campusconnect.model.ComplaintStatus;
import java.time.Instant;

public record HistoryDto(Long id, ComplaintStatus fromStatus, ComplaintStatus toStatus,
                         String changedByName, String note, Instant createdAt) {
}
