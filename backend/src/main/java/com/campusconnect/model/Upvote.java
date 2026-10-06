package com.campusconnect.model;

import jakarta.persistence.*;
import java.time.Instant;

/** "Me too" vote. One per user per complaint (enforced by a UNIQUE constraint). */
@Entity
@Table(name = "upvotes", uniqueConstraints = @UniqueConstraint(
        name = "uq_upvotes_complaint_user", columnNames = {"complaint_id", "user_id"}))
public class Upvote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Upvote() {
    }

    public Upvote(Complaint complaint, User user) {
        this.complaint = complaint;
        this.user = user;
    }

    public Long getId() { return id; }
    public Complaint getComplaint() { return complaint; }
    public User getUser() { return user; }
}
