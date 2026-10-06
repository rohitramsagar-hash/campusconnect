package com.campusconnect.dto;

import com.campusconnect.model.ComplaintStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StatusUpdateRequest(
        @NotNull(message = "Status is required.") ComplaintStatus status,
        @Size(max = 500, message = "Note can be at most 500 characters.") String note
) {
}
