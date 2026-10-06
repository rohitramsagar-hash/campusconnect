package com.campusconnect.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.time.ZoneOffset;

@Entity
@Table(name = "complaints", indexes = {
        @Index(name = "idx_complaints_status", columnList = "status"),
        @Index(name = "idx_complaints_created_by", columnList = "created_by_id"),
        @Index(name = "idx_complaints_assigned_to", columnList = "assigned_to_id"),
        @Index(name = "idx_complaints_created_at", columnList = "created_at")
})
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(length = 150)
    private String location;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Priority priority;

    /** Numeric copy of the priority so lists can be sorted URGENT > HIGH > MEDIUM > LOW. */
    @Column(name = "priority_rank", nullable = false)
    private int priorityRank;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ComplaintStatus status = ComplaintStatus.OPEN;

    /** Reporter's name is hidden from everyone except admins. */
    @Column(nullable = false)
    private boolean anonymous;

    /** Public complaints appear in the campus feed; private ones only to reporter, assignee and admins. */
    @Column(name = "is_public", nullable = false)
    private boolean publicVisible = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @Column(name = "upvote_count", nullable = false, updatable = false) // changed only by an atomic UPDATE query
    private int upvoteCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        priorityRank = priority.getRank();
        if (dueAt == null) {
            dueAt = createdAt.plus(priority.getResolutionTarget());
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
        priorityRank = priority.getRank();
    }

    /** Human friendly ticket number, e.g. CC-2026-00042. */
    public String getTicketNo() {
        int year = (createdAt == null ? Instant.now() : createdAt).atZone(ZoneOffset.UTC).getYear();
        return String.format("CC-%d-%05d", year, id);
    }

    public boolean isOverdue() {
        return status.isActive() && dueAt != null && dueAt.isBefore(Instant.now());
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public int getPriorityRank() { return priorityRank; }
    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }
    public boolean isAnonymous() { return anonymous; }
    public void setAnonymous(boolean anonymous) { this.anonymous = anonymous; }
    public boolean isPublicVisible() { return publicVisible; }
    public void setPublicVisible(boolean publicVisible) { this.publicVisible = publicVisible; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public User getAssignedTo() { return assignedTo; }
    public void setAssignedTo(User assignedTo) { this.assignedTo = assignedTo; }
    public int getUpvoteCount() { return upvoteCount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}
