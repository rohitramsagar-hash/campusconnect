package com.campusconnect.service;

import com.campusconnect.dto.*;
import com.campusconnect.exception.ApiException;
import com.campusconnect.model.*;
import com.campusconnect.repository.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ComplaintService {

    private static final Map<String, Sort> SORTS = Map.of(
            "newest", Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")),
            "oldest", Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id")),
            "upvotes", Sort.by(Sort.Order.desc("upvoteCount"), Sort.Order.desc("createdAt")),
            "priority", Sort.by(Sort.Order.desc("priorityRank"), Sort.Order.asc("createdAt")),
            "due", Sort.by(Sort.Order.asc("dueAt")));

    private final ComplaintRepository complaintRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final StatusHistoryRepository historyRepository;
    private final UpvoteRepository upvoteRepository;

    public ComplaintService(ComplaintRepository complaintRepository, CategoryRepository categoryRepository,
                            UserRepository userRepository, CommentRepository commentRepository,
                            StatusHistoryRepository historyRepository, UpvoteRepository upvoteRepository) {
        this.complaintRepository = complaintRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.historyRepository = historyRepository;
        this.upvoteRepository = upvoteRepository;
    }

    // ---- create ---------------------------------------------------------------------------

    public ComplaintDetail create(ComplaintRequest req, User actor) {
        Category category = categoryRepository.findById(req.categoryId())
                .filter(Category::isActive)
                .orElseThrow(() -> ApiException.badRequest("INVALID_CATEGORY", "Choose a valid category."));

        Complaint c = new Complaint();
        c.setTitle(req.title().trim());
        c.setDescription(req.description().trim());
        c.setLocation(AuthService.blankToNull(req.location()));
        c.setCategory(category);
        c.setPriority(req.priority());
        c.setAnonymous(req.anonymous());
        c.setPublicVisible(req.isPublic() == null || req.isPublic());
        c.setCreatedBy(actor);
        complaintRepository.save(c);
        historyRepository.save(new StatusHistory(c, null, ComplaintStatus.OPEN, actor, "Complaint submitted"));
        return toDetail(c, actor);
    }

    // ---- read -----------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<ComplaintSummary> list(User actor, String scope, ComplaintStatus status, Long categoryId,
                                               Priority priority, String q, String sort, int page, int size) {
        Sort order = SORTS.get(sort == null ? "newest" : sort);
        if (order == null) {
            throw ApiException.badRequest("INVALID_PARAMETER", "sort must be one of " + SORTS.keySet() + ".");
        }
        Specification<Complaint> spec = Specification.where(ComplaintSpecs.visibleTo(actor))
                .and(scopeSpec(scope, actor))
                .and(ComplaintSpecs.hasStatus(status))
                .and(ComplaintSpecs.hasPriority(priority))
                .and(ComplaintSpecs.inCategory(categoryId))
                .and(ComplaintSpecs.matches(q));

        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), order);
        Page<Complaint> result = complaintRepository.findAll(spec, pageable);
        return PageResponse.of(result, toSummaries(result.getContent(), actor));
    }

    @Transactional(readOnly = true)
    public ComplaintDetail get(Long id, User actor) {
        return toDetail(loadVisible(id, actor), actor);
    }

    // ---- workflow -------------------------------------------------------------------------

    public ComplaintDetail changeStatus(Long id, StatusUpdateRequest req, User actor) {
        Complaint c = loadVisible(id, actor);
        ComplaintStatus from = c.getStatus();
        ComplaintStatus to = req.status();
        List<ComplaintStatus> allowed = ComplaintPolicy.allowedNextStatuses(c, actor);

        if (allowed.isEmpty() && !from.isFinal()) {
            throw ApiException.forbidden(from == ComplaintStatus.RESOLVED
                    ? "Only the person who reported this (or an admin) can close or reopen it."
                    : "Only the assigned staff member or an admin can update this complaint.");
        }
        if (from == to) {
            throw ApiException.conflict("INVALID_TRANSITION", "The complaint is already " + label(to) + ".");
        }
        if (!allowed.contains(to)) {
            throw ApiException.conflict("INVALID_TRANSITION",
                    "A complaint can't move from " + label(from) + " to " + label(to) + ".");
        }
        String note = AuthService.blankToNull(req.note());
        if (ComplaintPolicy.noteRequired(from, to) && note == null) {
            throw ApiException.badRequest("NOTE_REQUIRED", to == ComplaintStatus.REJECTED
                    ? "Please give a reason for rejecting." : "Please say why you are reopening this complaint.");
        }

        c.setStatus(to);
        if (to == ComplaintStatus.RESOLVED) {
            c.setResolvedAt(Instant.now());
        } else if (from == ComplaintStatus.RESOLVED && to == ComplaintStatus.IN_PROGRESS) {
            c.setResolvedAt(null); // reopened
        }
        historyRepository.save(new StatusHistory(c, from, to, actor, note));
        return toDetail(c, actor);
    }

    public ComplaintDetail assign(Long id, AssignRequest req, User actor) {
        Complaint c = loadVisible(id, actor);
        if (!actor.isAdmin()) {
            throw ApiException.forbidden("Only admins can assign complaints.");
        }
        if (c.getStatus().isFinal()) {
            throw ApiException.conflict("INVALID_STATE", "A " + label(c.getStatus()) + " complaint can't be reassigned.");
        }
        User staff = userRepository.findById(req.staffId())
                .filter(u -> u.getRole() == Role.STAFF && u.isActive())
                .orElseThrow(() -> ApiException.badRequest("INVALID_ASSIGNEE", "Choose an active staff member."));
        if (ComplaintPolicy.isAssignee(c, staff)) {
            throw ApiException.conflict("ALREADY_ASSIGNED", "This complaint is already assigned to " + staff.getName() + ".");
        }
        c.setAssignedTo(staff);
        historyRepository.save(new StatusHistory(c, c.getStatus(), c.getStatus(), actor,
                "Assigned to " + staff.getName()));
        return toDetail(c, actor);
    }

    public CommentDto addComment(Long id, CommentRequest req, User actor) {
        Complaint c = loadVisible(id, actor);
        if (!ComplaintPolicy.canComment(c, actor)) {
            throw ApiException.forbidden("Only the reporter, the assigned staff member and admins can comment.");
        }
        Comment comment = commentRepository.save(new Comment(c, actor, req.message().trim()));
        return toCommentDto(comment);
    }

    public UpvoteResponse upvote(Long id, User actor) {
        Complaint c = loadVisible(id, actor);
        if (ComplaintPolicy.isOwner(c, actor)) {
            throw ApiException.badRequest("CANNOT_UPVOTE_OWN", "You can't upvote your own complaint.");
        }
        if (upvoteRepository.existsByComplaintIdAndUserId(id, actor.getId())) {
            throw ApiException.conflict("ALREADY_UPVOTED", "You have already upvoted this complaint.");
        }
        upvoteRepository.save(new Upvote(c, actor));
        complaintRepository.changeUpvoteCount(id, 1);
        return new UpvoteResponse(c.getUpvoteCount() + 1, true);
    }

    public UpvoteResponse removeUpvote(Long id, User actor) {
        Complaint c = loadVisible(id, actor);
        Upvote vote = upvoteRepository.findByComplaintIdAndUserId(id, actor.getId())
                .orElseThrow(() -> ApiException.notFound("Upvote"));
        upvoteRepository.delete(vote);
        complaintRepository.changeUpvoteCount(id, -1);
        return new UpvoteResponse(Math.max(c.getUpvoteCount() - 1, 0), false);
    }

    // ---- helpers --------------------------------------------------------------------------

    /** Private complaints the user may not see are reported as "not found" so their existence isn't leaked. */
    private Complaint loadVisible(Long id, User actor) {
        return complaintRepository.findById(id)
                .filter(c -> ComplaintPolicy.canView(c, actor))
                .orElseThrow(() -> ApiException.notFound("Complaint"));
    }

    private Specification<Complaint> scopeSpec(String scope, User actor) {
        if (scope == null || scope.isBlank() || scope.equals("all")) {
            return null;
        }
        return switch (scope) {
            case "mine" -> ComplaintSpecs.reportedBy(actor);
            case "assigned" -> ComplaintSpecs.assignedTo(actor);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                    "scope must be one of all, mine, assigned.");
        };
    }

    List<ComplaintSummary> toSummaries(List<Complaint> complaints, User actor) {
        if (complaints.isEmpty()) {
            return List.of();
        }
        Set<Long> upvoted = new HashSet<>(upvoteRepository.findUpvotedComplaintIds(
                actor.getId(), complaints.stream().map(Complaint::getId).toList()));
        return complaints.stream().map(c -> new ComplaintSummary(
                c.getId(), c.getTicketNo(), c.getTitle(), c.getCategory().getName(), c.getPriority(),
                c.getStatus(), c.getLocation(), reporterName(c, actor),
                c.getAssignedTo() == null ? null : c.getAssignedTo().getName(),
                c.getUpvoteCount(), upvoted.contains(c.getId()), c.isOverdue(), c.getCreatedAt(), c.getDueAt()))
                .toList();
    }

    private ComplaintDetail toDetail(Complaint c, User actor) {
        List<CommentDto> comments = c.getId() == null ? List.of()
                : commentRepository.findByComplaintIdOrderByCreatedAtAsc(c.getId()).stream()
                .map(this::toCommentDto).toList();
        List<HistoryDto> history = historyRepository.findByComplaintIdOrderByCreatedAtAscIdAsc(c.getId()).stream()
                .map(h -> new HistoryDto(h.getId(), h.getFromStatus(), h.getToStatus(), h.getChangedBy().getName(),
                        h.getNote(), h.getCreatedAt()))
                .toList();
        boolean upvoted = upvoteRepository.existsByComplaintIdAndUserId(c.getId(), actor.getId());
        Permissions permissions = new Permissions(
                ComplaintPolicy.isOwner(c, actor),
                ComplaintPolicy.canComment(c, actor),
                ComplaintPolicy.canAssign(c, actor),
                ComplaintPolicy.canUpvote(c, actor),
                ComplaintPolicy.allowedNextStatuses(c, actor));
        User assignee = c.getAssignedTo();
        return new ComplaintDetail(
                c.getId(), c.getTicketNo(), c.getTitle(), c.getDescription(), c.getCategory().getId(),
                c.getCategory().getName(), c.getPriority(), c.getStatus(), c.getLocation(), c.isAnonymous(),
                c.isPublicVisible(), reporterName(c, actor),
                assignee == null ? null : new PersonRef(assignee.getId(), assignee.getName()),
                c.getUpvoteCount(), upvoted, c.isOverdue(), c.getCreatedAt(), c.getUpdatedAt(), c.getDueAt(),
                c.getResolvedAt(), comments, history, permissions);
    }

    private CommentDto toCommentDto(Comment comment) {
        return new CommentDto(comment.getId(), comment.getAuthor().getName(), comment.getAuthor().getRole(),
                comment.getMessage(), comment.getCreatedAt());
    }

    /** Anonymous reporters are hidden from everyone except admins (and themselves). */
    private static String reporterName(Complaint c, User actor) {
        if (!c.isAnonymous()) {
            return c.getCreatedBy().getName();
        }
        if (ComplaintPolicy.isOwner(c, actor)) {
            return "You (anonymous)";
        }
        return actor.isAdmin() ? c.getCreatedBy().getName() + " (anonymous)" : "Anonymous";
    }

    private static String label(ComplaintStatus status) {
        return status.name().replace('_', ' ').toLowerCase();
    }
}
