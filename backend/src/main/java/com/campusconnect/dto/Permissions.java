package com.campusconnect.dto;

import com.campusconnect.model.ComplaintStatus;
import java.util.List;

/** Tells the UI which buttons to show. The server re-checks every action anyway. */
public record Permissions(
        boolean owner,
        boolean canComment,
        boolean canAssign,
        boolean canUpvote,
        List<ComplaintStatus> allowedStatuses
) {
}
