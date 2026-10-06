package com.campusconnect.controller;

import com.campusconnect.dto.*;
import com.campusconnect.model.ComplaintStatus;
import com.campusconnect.model.Priority;
import com.campusconnect.model.User;
import com.campusconnect.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    /** List with filters, e.g. /api/complaints?scope=mine&status=OPEN&q=wifi&sort=newest&page=0&size=10 */
    @GetMapping
    public PageResponse<ComplaintSummary> list(@AuthenticationPrincipal User user,
                                               @RequestParam(name = "scope", required = false) String scope,
                                               @RequestParam(name = "status", required = false) ComplaintStatus status,
                                               @RequestParam(name = "categoryId", required = false) Long categoryId,
                                               @RequestParam(name = "priority", required = false) Priority priority,
                                               @RequestParam(name = "q", required = false) String q,
                                               @RequestParam(name = "sort", defaultValue = "newest") String sort,
                                               @RequestParam(name = "page", defaultValue = "0") int page,
                                               @RequestParam(name = "size", defaultValue = "10") int size) {
        return complaintService.list(user, scope, status, categoryId, priority, q, sort, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintDetail create(@AuthenticationPrincipal User user, @Valid @RequestBody ComplaintRequest req) {
        return complaintService.create(req, user);
    }

    @GetMapping("/{id}")
    public ComplaintDetail get(@AuthenticationPrincipal User user, @PathVariable("id") Long id) {
        return complaintService.get(id, user);
    }

    @PatchMapping("/{id}/status")
    public ComplaintDetail changeStatus(@AuthenticationPrincipal User user, @PathVariable("id") Long id,
                                        @Valid @RequestBody StatusUpdateRequest req) {
        return complaintService.changeStatus(id, req, user);
    }

    @PatchMapping("/{id}/assign")
    public ComplaintDetail assign(@AuthenticationPrincipal User user, @PathVariable("id") Long id,
                                  @Valid @RequestBody AssignRequest req) {
        return complaintService.assign(id, req, user);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto comment(@AuthenticationPrincipal User user, @PathVariable("id") Long id,
                              @Valid @RequestBody CommentRequest req) {
        return complaintService.addComment(id, req, user);
    }

    @PostMapping("/{id}/upvote")
    public UpvoteResponse upvote(@AuthenticationPrincipal User user, @PathVariable("id") Long id) {
        return complaintService.upvote(id, user);
    }

    @DeleteMapping("/{id}/upvote")
    public UpvoteResponse removeUpvote(@AuthenticationPrincipal User user, @PathVariable("id") Long id) {
        return complaintService.removeUpvote(id, user);
    }
}
