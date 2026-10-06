package com.campusconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank(message = "Comment cannot be empty.")
        @Size(max = 1000, message = "Comment can be at most 1000 characters.")
        String message
) {
}
