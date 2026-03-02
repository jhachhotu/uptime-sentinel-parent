package com.sentinel.monitoring_service.controller;

import com.sentinel.monitoring_service.service.MonthlyReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportTestController {

    @Autowired
    private MonthlyReportService reportService;

    @GetMapping("/test-monthly")
    public String triggerTestReport() {
        try {
            reportService.generateMonthlyReport();
            return "SUCCESS: Monthly Report logic triggered. Check your console for logs and email.";
        } catch (Exception e) {
            return "ERROR: Failed to generate report: " + e.getMessage();
        }
    }
}