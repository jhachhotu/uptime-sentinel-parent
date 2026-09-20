package com.sentinel.monitoring_service.controller;

import com.sentinel.monitoring_service.entity.IssueCategory;
import com.sentinel.monitoring_service.entity.IssuePriority;
import com.sentinel.monitoring_service.entity.IssueStatus;
import com.sentinel.monitoring_service.entity.IssueTicket;
import com.sentinel.monitoring_service.service.IssueTicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring/issues")
public class IssueTicketController {

    private static final Logger log = LoggerFactory.getLogger(IssueTicketController.class);

    @Autowired
    private IssueTicketService issueTicketService;

    /**
     * Raise a new issue with optional screenshot / proof image.
     * Uploads proof directly to AWS S3 and invokes AWS Lambda.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> raiseIssue(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam(value = "category", required = false, defaultValue = "BUG") String categoryStr,
            @RequestParam(value = "priority", required = false, defaultValue = "MEDIUM") String priorityStr,
            @RequestParam(value = "websiteId", required = false) Long websiteId,
            @RequestParam(value = "proof", required = false) MultipartFile proofFile,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            String userEmail = userDetails != null ? userDetails.getUsername() : "anonymous@user.com";
            String userName = userEmail.split("@")[0];

            IssueCategory category = IssueCategory.valueOf(categoryStr.toUpperCase());
            IssuePriority priority = IssuePriority.valueOf(priorityStr.toUpperCase());

            log.info("User {} is raising an issue: '{}' with category: {}, priority: {}", userEmail, title, category, priority);

            IssueTicket createdTicket = issueTicketService.createIssue(
                    title,
                    description,
                    category,
                    priority,
                    websiteId,
                    userEmail,
                    userName,
                    proofFile
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(createdTicket);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid issue submission argument: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create issue ticket", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to raise issue: " + e.getMessage()));
        }
    }

    /**
     * Retrieve all issues reported by the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<IssueTicket>> getMyIssues(@AuthenticationPrincipal UserDetails userDetails) {
        String userEmail = userDetails.getUsername();
        return ResponseEntity.ok(issueTicketService.getUserIssues(userEmail));
    }

    /**
     * Retrieve all issues for a specific monitored website.
     */
    @GetMapping("/monitor/{websiteId}")
    public ResponseEntity<List<IssueTicket>> getIssuesByWebsite(@PathVariable Long websiteId) {
        return ResponseEntity.ok(issueTicketService.getIssuesForWebsite(websiteId));
    }

    /**
     * Retrieve a specific issue by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getIssueById(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        String userEmail = userDetails.getUsername();
        return issueTicketService.getIssueById(id)
                .map(ticket -> {
                    if (!userEmail.equals(ticket.getUserEmail())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied"));
                    }
                    return ResponseEntity.ok(ticket);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update the status of an issue (e.g. RESOLVED, INVESTIGATING, CLOSED).
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String statusStr = body.get("status");
        if (statusStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Status field is required"));
        }

        try {
            IssueStatus newStatus = IssueStatus.valueOf(statusStr.toUpperCase());
            String userEmail = userDetails.getUsername();

            return issueTicketService.updateStatus(id, newStatus, userEmail)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status value: " + statusStr));
        }
    }

    /**
     * Delete an issue.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteIssue(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        String userEmail = userDetails.getUsername();
        boolean deleted = issueTicketService.deleteIssue(id, userEmail);
        if (deleted) {
            return ResponseEntity.ok(Map.of("message", "Issue ticket deleted successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Issue ticket not found or you are not authorized to delete it"));
        }
    }
}
