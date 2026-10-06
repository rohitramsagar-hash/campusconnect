package com.campusconnect.model;

/**
 * Life cycle of a complaint:
 * OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED
 * OPEN / IN_PROGRESS -> REJECTED, RESOLVED -> IN_PROGRESS (reopened by the reporter)
 */
public enum ComplaintStatus {
    OPEN, IN_PROGRESS, RESOLVED, CLOSED, REJECTED;

    public boolean isActive() {
        return this == OPEN || this == IN_PROGRESS;
    }

    public boolean isFinal() {
        return this == CLOSED || this == REJECTED;
    }
}
