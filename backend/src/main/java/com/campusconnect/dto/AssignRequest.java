package com.campusconnect.dto;

import jakarta.validation.constraints.NotNull;

public record AssignRequest(@NotNull(message = "Choose a staff member.") Long staffId) {
}
