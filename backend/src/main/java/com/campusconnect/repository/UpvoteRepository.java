package com.campusconnect.repository;

import com.campusconnect.model.Upvote;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UpvoteRepository extends JpaRepository<Upvote, Long> {

    boolean existsByComplaintIdAndUserId(Long complaintId, Long userId);

    Optional<Upvote> findByComplaintIdAndUserId(Long complaintId, Long userId);

    @Query("select u.complaint.id from Upvote u where u.user.id = :userId and u.complaint.id in :ids")
    List<Long> findUpvotedComplaintIds(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);
}
