package com.campusconnect.repository;

import com.campusconnect.model.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = "author")
    List<Comment> findByComplaintIdOrderByCreatedAtAsc(Long complaintId);
}
