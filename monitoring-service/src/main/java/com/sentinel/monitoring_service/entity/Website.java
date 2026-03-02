package com.sentinel.monitoring_service.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "monitored_websites")
public class Website {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(nullable = false)
    private String url;

    // Monitor type: HTTP, SSL, KEYWORD, PORT, PING, API, DNS
    @Column(name = "monitor_type")
    private String monitorType = "HTTP";

    @Column(name = "check_interval")
    private int checkInterval = 5;

    private String status = "UNKNOWN";
    private LocalDateTime lastCheck;

    @Column(name = "response_time")
    private Long responseTime;

    // --- NOTIFICATIONS ---
    private String lastStatus = "UNKNOWN";
    private LocalDateTime downSince;

    @Column(name = "last_alert_sent_at")
    private LocalDateTime lastAlertSentAt;

    // --- USER OWNERSHIP ---
    @Column(name = "owner_id")
    private String ownerId;

    @Column(name = "owner_email")
    private String ownerEmail;

    // --- KEYWORD MONITORING ---
    private String keyword;

    // --- PORT MONITORING ---
    private Integer port;

    // --- API MONITORING ---
    @Column(name = "http_method")
    private String httpMethod = "GET";

    @Column(name = "http_body", columnDefinition = "TEXT")
    private String httpBody;

    // --- SSL MONITORING ---
    @Column(name = "ssl_expiry_date")
    private LocalDate sslExpiryDate;

    @Column(name = "ssl_days_remaining")
    private Integer sslDaysRemaining;

    // --- DNS MONITORING ---
    @Column(name = "dns_expected_ip")
    private String dnsExpectedIp;

    @Column(name = "dns_resolved_ip")
    private String dnsResolvedIp;

    // =================== GETTERS & SETTERS ===================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMonitorType() {
        return monitorType;
    }

    public void setMonitorType(String monitorType) {
        this.monitorType = monitorType;
    }

    public int getCheckInterval() {
        return checkInterval;
    }

    public void setCheckInterval(int checkInterval) {
        this.checkInterval = checkInterval;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastCheck() {
        return lastCheck;
    }

    public void setLastCheck(LocalDateTime lastCheck) {
        this.lastCheck = lastCheck;
    }

    public Long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(Long responseTime) {
        this.responseTime = responseTime;
    }

    public String getLastStatus() {
        return lastStatus;
    }

    public void setLastStatus(String lastStatus) {
        this.lastStatus = lastStatus;
    }

    public LocalDateTime getDownSince() {
        return downSince;
    }

    public void setDownSince(LocalDateTime downSince) {
        this.downSince = downSince;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getHttpBody() {
        return httpBody;
    }

    public void setHttpBody(String httpBody) {
        this.httpBody = httpBody;
    }

    public LocalDate getSslExpiryDate() {
        return sslExpiryDate;
    }

    public void setSslExpiryDate(LocalDate sslExpiryDate) {
        this.sslExpiryDate = sslExpiryDate;
    }

    public Integer getSslDaysRemaining() {
        return sslDaysRemaining;
    }

    public void setSslDaysRemaining(Integer sslDaysRemaining) {
        this.sslDaysRemaining = sslDaysRemaining;
    }

    public String getDnsExpectedIp() {
        return dnsExpectedIp;
    }

    public void setDnsExpectedIp(String dnsExpectedIp) {
        this.dnsExpectedIp = dnsExpectedIp;
    }

    public String getDnsResolvedIp() {
        return dnsResolvedIp;
    }

    public void setDnsResolvedIp(String dnsResolvedIp) {
        this.dnsResolvedIp = dnsResolvedIp;
    }

    public LocalDateTime getLastAlertSentAt() {
        return lastAlertSentAt;
    }

    public void setLastAlertSentAt(LocalDateTime lastAlertSentAt) {
        this.lastAlertSentAt = lastAlertSentAt;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }
}