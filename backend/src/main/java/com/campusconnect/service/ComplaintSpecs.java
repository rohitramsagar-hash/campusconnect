package com.campusconnect.service;

import com.campusconnect.model.Complaint;
import com.campusconnect.model.ComplaintStatus;
import com.campusconnect.model.Priority;
import com.campusconnect.model.User;
import jakarta.persistence.criteria.JoinType;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.data.jpa.domain.Specification;

/** Reusable WHERE-clause pieces for the complaint list. */
final class ComplaintSpecs {

    private static final Pattern TICKET = Pattern.compile("(?i)^\\s*CC-\\d{4}-(\\d{1,9})\\s*$");

    private ComplaintSpecs() {
    }

    static Specification<Complaint> visibleTo(User user) {
        if (user.isAdmin()) {
            return (root, q, cb) -> cb.conjunction();
        }
        return (root, q, cb) -> cb.or(
                cb.isTrue(root.get("publicVisible")),
                cb.equal(root.get("createdBy").get("id"), user.getId()),
                cb.equal(root.join("assignedTo", JoinType.LEFT).get("id"), user.getId()));
    }

    static Specification<Complaint> reportedBy(User user) {
        return (root, q, cb) -> cb.equal(root.get("createdBy").get("id"), user.getId());
    }

    static Specification<Complaint> assignedTo(User user) {
        return (root, q, cb) -> cb.equal(root.join("assignedTo", JoinType.LEFT).get("id"), user.getId());
    }

    static Specification<Complaint> hasStatus(ComplaintStatus status) {
        return status == null ? null : (root, q, cb) -> cb.equal(root.get("status"), status);
    }

    static Specification<Complaint> hasPriority(Priority priority) {
        return priority == null ? null : (root, q, cb) -> cb.equal(root.get("priority"), priority);
    }

    static Specification<Complaint> inCategory(Long categoryId) {
        return categoryId == null ? null : (root, q, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    /** Free-text search on title / description / location, or an exact ticket number like CC-2026-00012. */
    static Specification<Complaint> matches(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher ticket = TICKET.matcher(text);
        if (ticket.matches()) {
            long id = Long.parseLong(ticket.group(1));
            return (root, q, cb) -> cb.equal(root.get("id"), id);
        }
        String like = "%" + text.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return (root, q, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), like, '!'),
                cb.like(cb.lower(root.get("description")), like, '!'),
                cb.like(cb.lower(root.get("location")), like, '!'));
    }
}
