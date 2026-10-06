package com.campusconnect.dto;

import com.campusconnect.model.Role;
import java.time.Instant;

public record CommentDto(Long id, String authorName, Role authorRole, String message, Instant createdAt) {
}
