package com.campusconnect.dto;

import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(@NotNull(message = "active is required.") Boolean active) {
}
