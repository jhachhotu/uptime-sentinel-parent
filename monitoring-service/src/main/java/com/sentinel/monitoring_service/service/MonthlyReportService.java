package com.sentinel.monitoring_service.service;

import com.sentinel.monitoring_service.dto.WebsiteStatsDTO;
import com.sentinel.monitoring_service.entity.Website;
import com.sentinel.monitoring_service.repository.MonitoringLogRepository;
import com.sentinel.monitoring_service.repository.WebsiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class MonthlyReportService {

    @Autowired
    private WebsiteRepository websiteRepository;

    @Autowired
    private MonitoringLogRepository logRepository;

    @Autowired
    private EmailService emailService;

    // Runs at midnight on the 1st of every month
    @Scheduled(cron = "0 0 0 1 * *")
    public void generateMonthlyReport() {
        List<Website> allSites = websiteRepository.findAll();
        List<WebsiteStatsDTO> reportCards = new ArrayList<>();

        // Define the time window (last 30 days)
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);

        for (Website site : allSites) {
            WebsiteStatsDTO card = new WebsiteStatsDTO();
            card.setUrl(site.getUrl());

            // 1. Calculate Uptime %
            long totalChecks = logRepository.countByWebsiteIdAndTimestampAfter(site.getId(), oneMonthAgo);
            long upChecks = logRepository.countUpStatus(site.getId(), oneMonthAgo);

            double uptime = (totalChecks > 0) ? (upChecks * 100.0 / totalChecks) : 0.0;
            card.setUptimePercentage(Math.round(uptime * 1000.0) / 1000.0); // 3 decimal places

            // 2. Get Average Response Time
            Double avgRT = logRepository.getAverageResponseTime(site.getId(), oneMonthAgo);
            card.setAvgResponseTime(avgRT != null ? avgRT.longValue() : 0L);

            // 3. Count Incidents (Times it went DOWN)
            // For now, we count total 'DOWN' logs in the period
            long incidents = totalChecks - upChecks;
            card.setIncidentCount((int) incidents);

            reportCards.add(card);
        }

        // 4. Send the final report to your email
        emailService.sendMonthlyReport(reportCards);
    }
}