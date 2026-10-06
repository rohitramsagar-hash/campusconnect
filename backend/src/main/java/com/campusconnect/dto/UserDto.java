package com.campusconnect.dto;

import com.campusconnect.model.Role;
import com.campusconnect.model.User;
import java.time.Instant;

public record UserDto(Long id, String name, String email, Role role, String department,
                      boolean active, Instant createdAt) {

    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getDepartment(),
                u.isActive(), u.getCreatedAt());
    }
}
