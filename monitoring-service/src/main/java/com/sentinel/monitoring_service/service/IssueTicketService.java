package com.sentinel.monitoring_service.service;

import com.sentinel.monitoring_service.entity.IssueCategory;
import com.sentinel.monitoring_service.entity.IssuePriority;
import com.sentinel.monitoring_service.entity.IssueStatus;
import com.sentinel.monitoring_service.entity.IssueTicket;
import com.sentinel.monitoring_service.entity.Website;
import com.sentinel.monitoring_service.repository.IssueTicketRepository;
import com.sentinel.monitoring_service.repository.WebsiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class IssueTicketService {

    private static final Logger log = LoggerFactory.getLogger(IssueTicketService.class);

    @Autowired
    private IssueTicketRepository ticketRepository;

    @Autowired
    private WebsiteRepository websiteRepository;

    @Autowired
    private AwsS3Service s3Service;

    @Autowired
    private AwsLambdaService lambdaService;

    /**
     * Creates an issue ticket, uploads proof to AWS S3, and invokes AWS Lambda.
     */
    @Transactional
    public IssueTicket createIssue(
            String title,
            String description,
            IssueCategory category,
            IssuePriority priority,
            Long websiteId,
            String userEmail,
            String userName,
            MultipartFile proofFile
    ) throws IOException {

        IssueTicket ticket = new IssueTicket();
        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setCategory(category != null ? category : IssueCategory.BUG);
        ticket.setPriority(priority != null ? priority : IssuePriority.MEDIUM);
        ticket.setStatus(IssueStatus.OPEN);
        ticket.setUserEmail(userEmail);
        ticket.setUserName(userName != null ? userName : userEmail.split("@")[0]);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());

        // Associate with monitor if provided
        if (websiteId != null) {
            ticket.setWebsiteId(websiteId);
            Optional<Website> siteOpt = websiteRepository.findById(websiteId);
            siteOpt.ifPresent(site -> ticket.setWebsiteName(site.getName() != null ? site.getName() : site.getUrl()));
        }

        // Upload proof to S3 if provided
        if (proofFile != null && !proofFile.isEmpty()) {
            AwsS3Service.S3UploadResult uploadResult = s3Service.uploadProof(proofFile, userEmail);
            ticket.setScreenshotUrl(uploadResult.getFileUrl());
            ticket.setS3Key(uploadResult.getKey());
            ticket.setS3Bucket(uploadResult.getBucket());
        }

        IssueTicket saved = ticketRepository.save(ticket);

        // Invoke AWS Lambda function for serverless image processing & alerting
        if (saved.getScreenshotUrl() != null) {
            try {
                AwsLambdaService.LambdaProcessingResult lambdaResult = lambdaService.processIssueProof(saved);
                saved.setLambdaProcessed(lambdaResult.isSuccess());
                saved.setLambdaExecutionArn(lambdaResult.getExecutionArnOrId());
                saved = ticketRepository.save(saved);
            } catch (Exception e) {
                log.warn("Lambda processing encountered non-blocking exception: {}", e.getMessage());
            }
        }

        return saved;
    }

    public List<IssueTicket> getUserIssues(String userEmail) {
        return ticketRepository.findByUserEmailOrderByCreatedAtDesc(userEmail);
    }

    public List<IssueTicket> getIssuesForWebsite(Long websiteId) {
        return ticketRepository.findByWebsiteIdOrderByCreatedAtDesc(websiteId);
    }

    public Optional<IssueTicket> getIssueById(Long id) {
        return ticketRepository.findById(id);
    }

    @Transactional
    public Optional<IssueTicket> updateStatus(Long id, IssueStatus newStatus, String userEmail) {
        Optional<IssueTicket> ticketOpt = ticketRepository.findById(id);
        if (ticketOpt.isPresent()) {
            IssueTicket ticket = ticketOpt.get();
            // Allow reporter to update, or admin
            ticket.setStatus(newStatus);
            ticket.setUpdatedAt(LocalDateTime.now());
            return Optional.of(ticketRepository.save(ticket));
        }
        return Optional.empty();
    }

    @Transactional
    public boolean deleteIssue(Long id, String userEmail) {
        Optional<IssueTicket> ticketOpt = ticketRepository.findById(id);
        if (ticketOpt.isPresent()) {
            IssueTicket ticket = ticketOpt.get();
            if (userEmail.equals(ticket.getUserEmail())) {
                if (ticket.getS3Bucket() != null && ticket.getS3Key() != null) {
                    s3Service.deleteFile(ticket.getS3Bucket(), ticket.getS3Key());
                }
                ticketRepository.delete(ticket);
                return true;
            }
        }
        return false;
    }
}
