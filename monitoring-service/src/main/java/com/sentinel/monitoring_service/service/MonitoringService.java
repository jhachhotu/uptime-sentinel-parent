package com.sentinel.monitoring_service.service;

import com.sentinel.monitoring_service.entity.MonitoringLog;
import com.sentinel.monitoring_service.entity.Website;
import com.sentinel.monitoring_service.repository.MonitoringLogRepository;
import com.sentinel.monitoring_service.repository.WebsiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.net.ssl.HttpsURLConnection;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpConnectTimeoutException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class MonitoringService {

    @Autowired
    private WebsiteRepository repository;

    @Autowired
    private MonitoringLogRepository logRepository;

    @Autowired
    private EmailService emailService;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    // Runs every 10 seconds — checks ALL monitored websites
    @Scheduled(fixedRate = 10000)
    public void monitorAllWebsites() {
        List<Website> allSites = repository.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Website site : allSites) {
            try {
                checkWebsite(site, now);
            } catch (Exception e) {
                System.err.println("Monitor error for " + site.getUrl() + ": " + e.getMessage());
            }
        }
    }

    private void checkWebsite(Website site, LocalDateTime now) {
        String monitorType = site.getMonitorType() != null ? site.getMonitorType() : "HTTP";
        String currentStatus;
        String rootCause;
        long startTime = System.currentTimeMillis();

        // Dispatch to the correct check method based on monitor type
        CheckResult result = switch (monitorType.toUpperCase()) {
            case "SSL" -> checkSSL(site);
            case "KEYWORD" -> checkKeyword(site);
            case "PORT" -> checkPort(site);
            case "PING" -> checkPing(site);
            case "API" -> checkAPI(site);
            case "DNS" -> checkDNS(site);
            default -> checkHTTP(site);
        };

        currentStatus = result.status;
        rootCause = result.rootCause;
        long responseTime = System.currentTimeMillis() - startTime;

        // Save history log with root cause
        MonitoringLog log = new MonitoringLog();
        log.setWebsite(site);
        log.setStatus(currentStatus);
        log.setResponseTime(responseTime);
        log.setTimestamp(now);
        log.setRootCause(rootCause);
        logRepository.save(log);

        // State change detection & alerts (with 24hr throttle)
        String lastStatus = site.getLastStatus();

        if (("UP".equals(lastStatus) || "UNKNOWN".equals(lastStatus) || lastStatus == null)
                && "DOWN".equals(currentStatus)) {
            site.setDownSince(now);

            // Only send DOWN email if no alert was sent in the last 24 hours
            boolean shouldSendAlert = site.getLastAlertSentAt() == null
                    || Duration.between(site.getLastAlertSentAt(), now).toHours() >= 24;

            if (shouldSendAlert) {
                emailService.sendIncidentAlert(site.getUrl(), "DOWN", now.toString(), null, rootCause, site.getId(),
                        site.getOwnerEmail());
                site.setLastAlertSentAt(now);
            }
        } else if ("DOWN".equals(lastStatus) && "UP".equals(currentStatus)) {
            String durationText = "Unknown";
            if (site.getDownSince() != null) {
                Duration duration = Duration.between(site.getDownSince(), now);
                durationText = formatDuration(duration);
            }
            // Always send recovery email and reset the cooldown
            emailService.sendIncidentAlert(site.getUrl(), "UP", now.toString(), durationText, "Resolved", site.getId(),
                    site.getOwnerEmail());
            site.setDownSince(null);
            site.setLastAlertSentAt(null);
        }

        // Persist state for dashboard
        site.setLastStatus(currentStatus);
        site.setStatus(currentStatus);
        site.setLastCheck(now);
        site.setResponseTime(responseTime);
        repository.save(site);
    }

    // ─────────────────────── HTTP CHECK ───────────────────────
    private CheckResult checkHTTP(Website site) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(site.getUrl()))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,*/*")
                    .header("Cache-Control", "no-cache")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            System.out.println("HTTP Ping: " + site.getUrl() + " -> " + response.statusCode());

            if (response.statusCode() >= 200 && response.statusCode() < 400) {
                return new CheckResult("UP", "HTTP " + response.statusCode());
            }
            return new CheckResult("DOWN", "HTTP Error " + response.statusCode());
        } catch (HttpConnectTimeoutException e) {
            return new CheckResult("DOWN", "Connection Timeout");
        } catch (UnknownHostException e) {
            return new CheckResult("DOWN", "DNS / Address Not Found");
        } catch (Exception e) {
            return new CheckResult("DOWN", "Error: " + e.getMessage());
        }
    }

    // ─────────────────────── SSL CHECK ───────────────────────
    private CheckResult checkSSL(Website site) {
        try {
            String urlStr = site.getUrl();
            if (!urlStr.startsWith("https://")) {
                urlStr = "https://" + urlStr.replaceFirst("^http://", "");
            }

            URL url = new URL(urlStr);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.connect();

            X509Certificate[] certs = (X509Certificate[]) conn.getServerCertificates();
            conn.disconnect();

            if (certs.length > 0) {
                X509Certificate cert = certs[0];
                LocalDate expiry = cert.getNotAfter().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expiry);

                site.setSslExpiryDate(expiry);
                site.setSslDaysRemaining((int) daysRemaining);

                if (daysRemaining <= 0) {
                    return new CheckResult("DOWN", "SSL Certificate EXPIRED on " + expiry);
                } else if (daysRemaining <= 30) {
                    return new CheckResult("UP", "SSL expires in " + daysRemaining + " days (Warning)");
                }
                return new CheckResult("UP", "SSL valid until " + expiry + " (" + daysRemaining + " days)");
            }
            return new CheckResult("DOWN", "No SSL certificate found");
        } catch (Exception e) {
            return new CheckResult("DOWN", "SSL Error: " + e.getMessage());
        }
    }

    // ─────────────────────── KEYWORD CHECK ───────────────────────
    private CheckResult checkKeyword(Website site) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(site.getUrl()))
                    .header("User-Agent", "Mozilla/5.0 Sentinel/1.0")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 400) {
                String body = response.body();
                String keyword = site.getKeyword();

                if (keyword != null && !keyword.isEmpty()) {
                    if (body.toLowerCase().contains(keyword.toLowerCase())) {
                        return new CheckResult("UP", "Keyword '" + keyword + "' found on page");
                    } else {
                        return new CheckResult("DOWN", "Keyword '" + keyword + "' NOT found on page");
                    }
                }
                return new CheckResult("UP", "Page loaded (no keyword set)");
            }
            return new CheckResult("DOWN", "HTTP Error " + response.statusCode());
        } catch (Exception e) {
            return new CheckResult("DOWN", "Keyword check failed: " + e.getMessage());
        }
    }

    // ─────────────────────── PORT CHECK ───────────────────────
    private CheckResult checkPort(Website site) {
        int port = site.getPort() != null ? site.getPort() : 80;
        String host;
        try {
            // Resolve domain using URI.create().toURL()
            java.net.URL urlObj = java.net.URI.create(site.getUrl()).toURL();
            host = urlObj.getHost();
            if (host == null || host.isEmpty()) { // Added check for empty host
                host = site.getUrl().replaceAll("https?://", "").split("[:/]")[0];
            }
        } catch (Exception e) {
            host = site.getUrl().replaceAll("https?://", "").split("[:/]")[0];
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 10000);
            return new CheckResult("UP", "Port " + port + " is OPEN on " + host);
        } catch (Exception e) {
            return new CheckResult("DOWN", "Port " + port + " is CLOSED on " + host);
        }
    }

    // ─────────────────────── PING CHECK ───────────────────────
    private CheckResult checkPing(Website site) {
        String host;
        try {
            URI uri = URI.create(site.getUrl());
            host = uri.getHost();
            if (host == null)
                host = site.getUrl().replaceAll("https?://", "").split("[:/]")[0];
        } catch (Exception e) {
            host = site.getUrl().replaceAll("https?://", "").split("[:/]")[0];
        }

        try {
            InetAddress address = InetAddress.getByName(host);
            boolean reachable = address.isReachable(10000);
            if (reachable) {
                return new CheckResult("UP", "Host " + host + " is reachable");
            }
            return new CheckResult("DOWN", "Host " + host + " is unreachable");
        } catch (UnknownHostException e) {
            return new CheckResult("DOWN", "DNS / Address Not Found: " + host);
        } catch (Exception e) {
            return new CheckResult("DOWN", "Ping failed: " + e.getMessage());
        }
    }

    // ─────────────────────── API CHECK ───────────────────────
    private CheckResult checkAPI(Website site) {
        try {
            String method = site.getHttpMethod() != null ? site.getHttpMethod().toUpperCase() : "GET";
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(site.getUrl()))
                    .header("User-Agent", "Sentinel-API-Monitor/1.0")
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10));

            // Set HTTP method and body
            switch (method) {
                case "POST" -> requestBuilder.POST(
                        HttpRequest.BodyPublishers.ofString(site.getHttpBody() != null ? site.getHttpBody() : ""));
                case "PUT" -> requestBuilder.PUT(
                        HttpRequest.BodyPublishers.ofString(site.getHttpBody() != null ? site.getHttpBody() : ""));
                case "DELETE" -> requestBuilder.DELETE();
                default -> requestBuilder.GET();
            }

            HttpResponse<Void> response = httpClient.send(requestBuilder.build(),
                    HttpResponse.BodyHandlers.discarding());
            System.out.println("API Check: " + method + " " + site.getUrl() + " -> " + response.statusCode());

            if (response.statusCode() >= 200 && response.statusCode() < 400) {
                return new CheckResult("UP", method + " " + response.statusCode() + " OK");
            }
            return new CheckResult("DOWN", method + " returned HTTP " + response.statusCode());
        } catch (Exception e) {
            return new CheckResult("DOWN", "API Error: " + e.getMessage());
        }
    }

    // ─────────────────────── DNS CHECK ───────────────────────
    private CheckResult checkDNS(Website site) {
        String host;
        try {
            URI uri = URI.create(site.getUrl());
            host = uri.getHost();
            if (host == null)
                host = site.getUrl().replaceAll("https?://", "").split("[:/]")[0];
        } catch (Exception e) {
            host = site.getUrl().replaceAll("https?://", "").split("[:/]")[0];
        }

        try {
            InetAddress address = InetAddress.getByName(host);
            String resolvedIp = address.getHostAddress();
            site.setDnsResolvedIp(resolvedIp);

            String expectedIp = site.getDnsExpectedIp();
            if (expectedIp != null && !expectedIp.isEmpty()) {
                if (resolvedIp.equals(expectedIp)) {
                    return new CheckResult("UP", "DNS resolves to " + resolvedIp + " (matches expected)");
                } else {
                    return new CheckResult("DOWN", "DNS mismatch: expected " + expectedIp + " but got " + resolvedIp);
                }
            }
            return new CheckResult("UP", "DNS resolves to " + resolvedIp);
        } catch (UnknownHostException e) {
            return new CheckResult("DOWN", "DNS resolution failed for " + host);
        } catch (Exception e) {
            return new CheckResult("DOWN", "DNS Error: " + e.getMessage());
        }
    }

    // ─────────────────────── HELPERS ───────────────────────
    private String formatDuration(Duration d) {
        long hours = d.toHours();
        long minutes = d.toMinutesPart();
        if (hours > 0)
            return hours + "h " + minutes + "m";
        return minutes + "m";
    }

    // Simple result holder
    private record CheckResult(String status, String rootCause) {
    }
}