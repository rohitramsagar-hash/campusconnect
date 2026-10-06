package com.campusconnect.repository;

import com.campusconnect.model.Complaint;
import com.campusconnect.model.ComplaintStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComplaintRepository extends JpaRepository<Complaint, Long>, JpaSpecificationExecutor<Complaint> {

    /** Loads category, reporter and assignee in the same query (avoids N+1 selects on list pages). */
    @Override
    @EntityGraph(attributePaths = {"category", "createdBy", "assignedTo"})
    Page<Complaint> findAll(Specification<Complaint> spec, Pageable pageable);

    @Modifying
    @Query("update Complaint c set c.upvoteCount = c.upvoteCount + :delta where c.id = :id")
    void changeUpvoteCount(@Param("id") Long id, @Param("delta") int delta);

    // ---- dashboard -------------------------------------------------------------------
    // scope: ALL (admin), OWN (complaints I reported), ASSIGNED (complaints assigned to me)

    String FROM_SCOPED = " from Complaint c left join c.assignedTo a"
            + " where (:scope = 'ALL' or (:scope = 'OWN' and c.createdBy.id = :uid)"
            + " or (:scope = 'ASSIGNED' and a.id = :uid))";

    @Query("select c.status, count(c)" + FROM_SCOPED + " group by c.status")
    List<Object[]> countByStatus(@Param("scope") String scope, @Param("uid") Long uid);

    @Query("select c.category.name, count(c)" + FROM_SCOPED + " group by c.category.name order by count(c) desc")
    List<Object[]> countByCategory(@Param("scope") String scope, @Param("uid") Long uid);

    @Query("select c.priority, count(c)" + FROM_SCOPED + " group by c.priority")
    List<Object[]> countByPriority(@Param("scope") String scope, @Param("uid") Long uid);

    @Query("select count(c)" + FROM_SCOPED + " and c.status in :active and c.dueAt < :now")
    long countOverdue(@Param("scope") String scope, @Param("uid") Long uid,
                      @Param("active") Collection<ComplaintStatus> active, @Param("now") Instant now);

    @Query("select c.createdAt, c.resolvedAt" + FROM_SCOPED + " and c.resolvedAt is not null")
    List<Object[]> resolutionTimes(@Param("scope") String scope, @Param("uid") Long uid);
}
