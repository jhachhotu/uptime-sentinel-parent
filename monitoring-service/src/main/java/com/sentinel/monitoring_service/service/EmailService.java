package com.sentinel.monitoring_service.service;

import com.sentinel.monitoring_service.dto.WebsiteStatsDTO;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @org.springframework.beans.factory.annotation.Value("${FRONTEND_URL:http://localhost:5173}")
    private String frontendUrl;

    private static final String DEFAULT_TO = "chhotusimaria@gmail.com";

    /**
     * Sends welcome email when a user logs in for the first time.
     */
    public void sendWelcomeEmail(String toEmail, String userName) {
        try {
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("frontendUrl", frontendUrl);

            String htmlContent = templateEngine.process("welcome-email", context);
            sendHtmlEmail(toEmail, "Welcome to Sentinel Monitoring!", htmlContent);
        } catch (Exception e) {
            System.err.println("Sentinel Error: Failed to send welcome email: " + e.getMessage());
        }
    }

    /**
     * Sends monthly business intelligence report.
     */
    public void sendMonthlyReport(List<WebsiteStatsDTO> stats) {
        try {
            Context context = new Context();
            context.setVariable("stats", stats);
            context.setVariable("frontendUrl", frontendUrl);

            String htmlContent = templateEngine.process("monthly-report", context);
            sendHtmlEmail(DEFAULT_TO, "Sentinel Monthly Report", htmlContent);
        } catch (Exception e) {
            System.err.println("Sentinel Error: Failed to send monthly report: " + e.getMessage());
        }
    }

    /**
     * Sends professional real-time incident alerts (UP/DOWN) to the monitor owner.
     */
    public void sendIncidentAlert(String url, String status, String time, String duration, String cause, Long siteId,
            String toEmail) {
        try {
            Context context = new Context();
            context.setVariable("url", url);
            context.setVariable("status", status);
            context.setVariable("time", time);
            context.setVariable("duration", duration);
            context.setVariable("cause", cause != null ? cause : "Connection Timeout");
            context.setVariable("siteId", siteId != null ? siteId : 0);
            context.setVariable("frontendUrl", frontendUrl);

            String htmlContent = templateEngine.process("incident-alert", context);

            // Send to the monitor owner's email, fallback to default
            String recipient = (toEmail != null && !toEmail.isEmpty()) ? toEmail : DEFAULT_TO;
            String subject = "Sentinel Alert: " + url + " is " + status;
            sendHtmlEmail(recipient, subject, htmlContent);
            System.out.println("Sentinel: Alert sent to " + recipient + " for " + url);
        } catch (Exception e) {
            System.err.println("Sentinel Error: Failed to send incident alert: " + e.getMessage());
        }
    }

    /**
     * Helper method to handle MimeMessage creation and sending.
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
}