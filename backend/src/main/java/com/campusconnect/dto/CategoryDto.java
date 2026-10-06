package com.campusconnect.dto;

import com.campusconnect.model.Category;

public record CategoryDto(Long id, String name, String description, boolean active) {

    public static CategoryDto from(Category c) {
        return new CategoryDto(c.getId(), c.getName(), c.getDescription(), c.isActive());
    }
}
