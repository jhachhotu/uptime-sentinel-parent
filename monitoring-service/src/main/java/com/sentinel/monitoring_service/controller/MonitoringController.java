package com.sentinel.monitoring_service.controller;

import com.sentinel.monitoring_service.dto.WebsiteStatsDTO;
import com.sentinel.monitoring_service.entity.IncidentComment;
import com.sentinel.monitoring_service.entity.MonitoringLog;
import com.sentinel.monitoring_service.entity.Website;
import com.sentinel.monitoring_service.repository.IncidentCommentRepository;
import com.sentinel.monitoring_service.repository.MonitoringLogRepository;
import com.sentinel.monitoring_service.repository.WebsiteRepository;
import com.sentinel.monitoring_service.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    @Autowired
    private WebsiteRepository repository;

    @Autowired
    private MonitoringLogRepository logRepository;

    @Autowired
    private IncidentCommentRepository commentRepository;

    @Autowired
    private EmailService emailService;

    // ─── Add a new website to monitor (stamps with owner ID) ───
    @PostMapping("/add")
    public ResponseEntity<Website> addWebsite(@RequestBody Website website,
            @AuthenticationPrincipal UserDetails userDetails) {
        String userEmail = userDetails.getUsername();

        website.setStatus("UNKNOWN");
        website.setLastStatus("UNKNOWN");
        website.setOwnerId(userEmail); // Using email as the primary owner identifier
        website.setOwnerEmail(userEmail);
        if (website.getCheckInterval() == 0) {
            website.setCheckInterval(5);
        }

        // Auto-assign any orphaned monitors (no ownerId) to this user on first add
        List<Website> orphans = repository.findOrphanedWebsites();
        if (!orphans.isEmpty()) {
            for (Website orphan : orphans) {
                orphan.setOwnerId(userEmail);
                orphan.setOwnerEmail(userEmail);
            }
            repository.saveAll(orphans);
        }

        Website savedWebsite = repository.save(website);
        return ResponseEntity.ok(savedWebsite);
    }

    // ─── List only current user's monitors ───
    @GetMapping("/all")
    public List<Website> getAllWebsites(@AuthenticationPrincipal UserDetails userDetails) {
        String userEmail = userDetails.getUsername();

        // Auto-assign orphaned monitors on first load
        List<Website> orphans = repository.findOrphanedWebsites();
        if (!orphans.isEmpty()) {
            for (Website orphan : orphans) {
                orphan.setOwnerId(userEmail);
                orphan.setOwnerEmail(userEmail);
            }
            repository.saveAll(orphans);
        }

        return repository.findByOwnerId(userEmail);
    }

    // ─── Get single website by ID (verify ownership) ───
    @GetMapping("/{id}")
    public ResponseEntity<Website> getWebsiteById(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        String userEmail = userDetails.getUsername();
        return repository.findById(id)
                .filter(site -> userEmail.equals(site.getOwnerId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── Get monitoring logs for a website (verify ownership) ───
    @GetMapping("/{id}/logs")
    public ResponseEntity<List<Map<String, Object>>> getWebsiteLogs(
            @PathVariable Long id,
            @RequestParam(defaultValue = "24") int hours,
            @AuthenticationPrincipal UserDetails userDetails) {

        String userEmail = userDetails.getUsername();
        Optional<Website> siteOpt = repository.findById(id);
        if (siteOpt.isEmpty() || !userEmail.equals(siteOpt.get().getOwnerId())) {
            return ResponseEntity.notFound().build();
        }

        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<MonitoringLog> logs = logRepository.findByWebsiteIdAndTimestampAfterOrderByTimestampDesc(id, since);

        List<Map<String, Object>> result = new ArrayList<>();
        for (MonitoringLog log : logs) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", log.getId());
            entry.put("status", log.getStatus());
            entry.put("responseTime", log.getResponseTime());
            entry.put("rootCause", log.getRootCause());
            entry.put("timestamp", log.getTimestamp().toString());
            result.add(entry);
        }
        return ResponseEntity.ok(result);
    }

    // ─── Public Status Page API (no auth required) ───
    @GetMapping("/status")
    public ResponseEntity<List<Map<String, Object>>> getPublicStatus() {
        List<Website> allSites = repository.findAll();
        List<Map<String, Object>> statusList = new ArrayList<>();

        for (Website site : allSites) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", site.getId());
            entry.put("name", site.getName());
            entry.put("url", site.getUrl());
            entry.put("monitorType", site.getMonitorType());
            entry.put("status", site.getStatus());
            entry.put("responseTime", site.getResponseTime());
            entry.put("lastCheck", site.getLastCheck() != null ? site.getLastCheck().toString() : null);
            entry.put("sslDaysRemaining", site.getSslDaysRemaining());
            statusList.add(entry);
        }
        return ResponseEntity.ok(statusList);
    }

    // ─── Business Analytics: Stats for current user's websites only ───
    @GetMapping("/stats")
    public ResponseEntity<List<WebsiteStatsDTO>> getBusinessStats(@AuthenticationPrincipal UserDetails userDetails) {
        String userEmail = userDetails.getUsername();
        List<Website> userSites = repository.findByOwnerId(userEmail);
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusDays(30);

        List<WebsiteStatsDTO> statsList = new ArrayList<>();

        for (Website site : userSites) {
            WebsiteStatsDTO stats = new WebsiteStatsDTO();
            stats.setUrl(site.getUrl());
            stats.setName(site.getName());
            stats.setStatus(site.getStatus());

            // Uptime %
            long totalChecks = logRepository.countByWebsiteIdAndTimestampAfter(site.getId(), oneMonthAgo);
            long upChecks = logRepository.countUpStatus(site.getId(), oneMonthAgo);
            double uptime = (totalChecks > 0) ? (upChecks * 100.0 / totalChecks) : 0.0;
            stats.setUptimePercentage(Math.round(uptime * 100.0) / 100.0);

            // Avg Response Time
            Double avgRT = logRepository.getAverageResponseTime(site.getId(), oneMonthAgo);
            stats.setAvgResponseTime(avgRT != null ? avgRT.longValue() : 0L);

            // Incident Count (DOWN logs in period)
            long incidents = totalChecks - upChecks;
            stats.setIncidentCount((int) incidents);

            // Total Downtime estimate
            if (site.getDownSince() != null) {
                long downMinutes = java.time.Duration.between(site.getDownSince(), LocalDateTime.now()).toMinutes();
                stats.setTotalDowntime(downMinutes + " min");
            } else {
                stats.setTotalDowntime("0 min");
            }

            statsList.add(stats);
        }

        return ResponseEntity.ok(statsList);
    }

    // ─── Remove a website and ALL its logs (verify ownership) ───
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<String> removeWebsite(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        String userEmail = userDetails.getUsername();
        Optional<Website> siteOpt = repository.findById(id);

        if (siteOpt.isEmpty() || !userEmail.equals(siteOpt.get().getOwnerId())) {
            return ResponseEntity.notFound().build();
        }

        logRepository.deleteByWebsiteId(id);
        repository.deleteById(id);
        return ResponseEntity.ok("Website and all associated logs removed successfully");
    }

    // ─── Welcome Email (called from frontend on first login) ───
    @PostMapping("/welcome")
    public ResponseEntity<String> sendWelcome(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        String name = body.get("name");

        if (name == null || name.isEmpty()) {
            name = email.split("@")[0];
        }

        System.out.println("Sentinel: Sending welcome email to " + email + " (name: " + name + ")");
        emailService.sendWelcomeEmail(email, name);
        return ResponseEntity.ok("Welcome email sent to " + email);
    }

    // ─── Add a comment to an incident ───
    @PostMapping("/{id}/comments")
    public ResponseEntity<IncidentComment> addComment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        String userEmail = userDetails.getUsername();
        String userName = userEmail.split("@")[0]; // Simplified generic logic for now

        IncidentComment comment = new IncidentComment();
        comment.setWebsiteId(id);
        comment.setUserId(userEmail);
        comment.setUserName(userName);
        comment.setMessage(body.get("message"));
        comment.setCreatedAt(LocalDateTime.now());

        return ResponseEntity.ok(commentRepository.save(comment));
    }

    // ─── Get comments for an incident ───
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<IncidentComment>> getComments(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(commentRepository.findByWebsiteIdOrderByCreatedAtDesc(id));
    }
}
