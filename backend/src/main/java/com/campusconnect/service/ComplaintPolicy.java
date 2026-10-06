package com.campusconnect.service;

import com.campusconnect.model.Complaint;
import com.campusconnect.model.ComplaintStatus;
import com.campusconnect.model.User;
import java.util.List;
import java.util.Objects;

/** All "who may do what" rules for complaints live here, in one place. */
public final class ComplaintPolicy {

    private ComplaintPolicy() {
    }

    public static boolean isOwner(Complaint c, User u) {
        return Objects.equals(c.getCreatedBy().getId(), u.getId());
    }

    public static boolean isAssignee(Complaint c, User u) {
        return c.getAssignedTo() != null && Objects.equals(c.getAssignedTo().getId(), u.getId());
    }

    /** Public complaints are visible to everyone; private ones only to reporter, assignee and admins. */
    public static boolean canView(Complaint c, User u) {
        return c.isPublicVisible() || u.isAdmin() || isOwner(c, u) || isAssignee(c, u);
    }

    public static boolean canComment(Complaint c, User u) {
        return u.isAdmin() || isOwner(c, u) || isAssignee(c, u);
    }

    public static boolean canAssign(Complaint c, User u) {
        return u.isAdmin() && !c.getStatus().isFinal();
    }

    public static boolean canUpvote(Complaint c, User u) {
        return canView(c, u) && !isOwner(c, u);
    }

    /**
     * Status workflow:
     * OPEN        -> IN_PROGRESS, REJECTED   (assigned staff or admin)
     * IN_PROGRESS -> RESOLVED, REJECTED      (assigned staff or admin)
     * RESOLVED    -> CLOSED, IN_PROGRESS     (reporter or admin: confirm fix, or reopen)
     * CLOSED, REJECTED                       final
     */
    public static List<ComplaintStatus> allowedNextStatuses(Complaint c, User u) {
        boolean handler = u.isAdmin() || isAssignee(c, u);
        boolean reporterSide = u.isAdmin() || isOwner(c, u);
        return switch (c.getStatus()) {
            case OPEN -> handler ? List.of(ComplaintStatus.IN_PROGRESS, ComplaintStatus.REJECTED) : List.of();
            case IN_PROGRESS -> handler ? List.of(ComplaintStatus.RESOLVED, ComplaintStatus.REJECTED) : List.of();
            case RESOLVED -> reporterSide ? List.of(ComplaintStatus.CLOSED, ComplaintStatus.IN_PROGRESS) : List.of();
            case CLOSED, REJECTED -> List.of();
        };
    }

    /** Rejecting or reopening must always explain why. */
    public static boolean noteRequired(ComplaintStatus from, ComplaintStatus to) {
        return to == ComplaintStatus.REJECTED || (from == ComplaintStatus.RESOLVED && to == ComplaintStatus.IN_PROGRESS);
    }
}
