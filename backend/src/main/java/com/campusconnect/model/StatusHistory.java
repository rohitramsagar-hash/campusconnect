package com.campusconnect.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

/** Append-only audit trail: every status change and assignment is recorded here. */
@Entity
@Table(name = "status_history", indexes = @Index(name = "idx_history_complaint", columnList = "complaint_id"))
public class StatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "from_status", length = 20)
    private ComplaintStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "to_status", nullable = false, length = 20)
    private ComplaintStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by_id", nullable = false)
    private User changedBy;

    @Column(length = 500)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public StatusHistory() {
    }

    public StatusHistory(Complaint complaint, ComplaintStatus fromStatus, ComplaintStatus toStatus,
                         User changedBy, String note) {
        this.complaint = complaint;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedBy = changedBy;
        this.note = note;
    }

    public Long getId() { return id; }
    public Complaint getComplaint() { return complaint; }
    public ComplaintStatus getFromStatus() { return fromStatus; }
    public ComplaintStatus getToStatus() { return toStatus; }
    public User getChangedBy() { return changedBy; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
