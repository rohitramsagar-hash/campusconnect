package com.campusconnect.dto;

import com.campusconnect.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Used by admins to create staff / admin accounts. */
public record CreateUserRequest(
        @NotBlank(message = "Name is required.") @Size(max = 100) String name,
        @NotBlank(message = "Email is required.") @Email(message = "Enter a valid email address.") @Size(max = 150)
        String email,
        @NotBlank(message = "Password is required.")
        @Pattern(regexp = ValidationRules.PASSWORD_REGEX, message = ValidationRules.PASSWORD_MESSAGE)
        String password,
        @NotNull(message = "Role is required.") Role role,
        @Size(max = 100) String department
) {
}
