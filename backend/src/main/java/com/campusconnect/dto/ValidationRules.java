package com.campusconnect.dto;

public final class ValidationRules {

    /** At least 8 characters, at least one letter and one digit. */
    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$";
    public static final String PASSWORD_MESSAGE =
            "Password must be 8-64 characters with at least one letter and one number.";

    private ValidationRules() {
    }
}
