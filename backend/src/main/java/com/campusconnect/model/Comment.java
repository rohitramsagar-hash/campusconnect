package com.campusconnect.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "comments", indexes = @Index(name = "idx_comments_complaint", columnList = "complaint_id"))
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Comment() {
    }

    public Comment(Complaint complaint, User author, String message) {
        this.complaint = complaint;
        this.author = author;
        this.message = message;
    }

    public Long getId() { return id; }
    public Complaint getComplaint() { return complaint; }
    public User getAuthor() { return author; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
