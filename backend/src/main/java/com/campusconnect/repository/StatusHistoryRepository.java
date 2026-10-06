package com.campusconnect.repository;

import com.campusconnect.model.StatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {

    @EntityGraph(attributePaths = "changedBy")
    List<StatusHistory> findByComplaintIdOrderByCreatedAtAscIdAsc(Long complaintId);
}
