package com.campusconnect.model;

import java.time.Duration;

/** Each priority has a resolution target (SLA). Complaints past it are shown as overdue. */
public enum Priority {
    LOW(1, Duration.ofDays(14)),
    MEDIUM(2, Duration.ofDays(7)),
    HIGH(3, Duration.ofDays(3)),
    URGENT(4, Duration.ofDays(1));

    private final int rank;
    private final Duration resolutionTarget;

    Priority(int rank, Duration resolutionTarget) {
        this.rank = rank;
        this.resolutionTarget = resolutionTarget;
    }

    public int getRank() {
        return rank;
    }

    public Duration getResolutionTarget() {
        return resolutionTarget;
    }
}
