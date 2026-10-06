package com.campusconnect.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Name is required.")
        @Size(max = 100, message = "Name can be at most 100 characters.")
        String name,

        @NotBlank(message = "Email is required.")
        @Email(message = "Enter a valid email address.")
        @Size(max = 150, message = "Email can be at most 150 characters.")
        String email,

        @NotBlank(message = "Password is required.")
        @Pattern(regexp = ValidationRules.PASSWORD_REGEX, message = ValidationRules.PASSWORD_MESSAGE)
        String password,

        @Size(max = 100, message = "Department can be at most 100 characters.")
        String department
) {
}
