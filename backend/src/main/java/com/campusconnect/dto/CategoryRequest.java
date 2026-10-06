package com.campusconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "Category name is required.")
        @Size(max = 60, message = "Category name can be at most 60 characters.")
        String name,
        @Size(max = 255, message = "Description can be at most 255 characters.")
        String description,
        Boolean active
) {
}
