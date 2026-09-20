package com.sentinel.monitoring_service.service;

import com.sentinel.monitoring_service.entity.IssueCategory;
import com.sentinel.monitoring_service.entity.IssuePriority;
import com.sentinel.monitoring_service.entity.IssueStatus;
import com.sentinel.monitoring_service.entity.IssueTicket;
import com.sentinel.monitoring_service.repository.IssueTicketRepository;
import com.sentinel.monitoring_service.repository.WebsiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IssueTicketServiceTest {

    @Mock
    private IssueTicketRepository ticketRepository;

    @Mock
    private WebsiteRepository websiteRepository;

    @Mock
    private AwsS3Service s3Service;

    @Mock
    private AwsLambdaService lambdaService;

    @InjectMocks
    private IssueTicketService issueTicketService;

    private MockMultipartFile sampleScreenshot;

    @BeforeEach
    void setUp() {
        sampleScreenshot = new MockMultipartFile(
                "proof",
                "test-error.png",
                "image/png",
                "fake image content".getBytes()
        );
    }

    @Test
    void testCreateIssueWithProof() throws IOException {
        when(s3Service.uploadProof(any(), eq("test@example.com"))).thenReturn(
                new AwsS3Service.S3UploadResult(
                        "sentinel-issue-proofs",
                        "issues/test@example.com/test-error.png",
                        "https://sentinel-issue-proofs.s3.us-east-1.amazonaws.com/issues/test@example.com/test-error.png",
                        true
                )
        );

        when(ticketRepository.save(any(IssueTicket.class))).thenAnswer(invocation -> {
            IssueTicket ticket = invocation.getArgument(0);
            ticket.setId(1L);
            return ticket;
        });

        when(lambdaService.processIssueProof(any(IssueTicket.class))).thenReturn(
                new AwsLambdaService.LambdaProcessingResult(true, "AWS_LAMBDA_OK: sentinel-issue-processor", "Processed successfully")
        );

        IssueTicket result = issueTicketService.createIssue(
                "Payment API 500 error",
                "Checkout fails on stage 3",
                IssueCategory.BUG,
                IssuePriority.HIGH,
                null,
                "test@example.com",
                "Tester",
                sampleScreenshot
        );

        assertNotNull(result);
        assertEquals("Payment API 500 error", result.getTitle());
        assertEquals("https://sentinel-issue-proofs.s3.us-east-1.amazonaws.com/issues/test@example.com/test-error.png", result.getScreenshotUrl());
        assertEquals(IssueStatus.OPEN, result.getStatus());
        assertTrue(result.getLambdaProcessed());
        assertEquals("AWS_LAMBDA_OK: sentinel-issue-processor", result.getLambdaExecutionArn());

        verify(s3Service, times(1)).uploadProof(any(), eq("test@example.com"));
        verify(lambdaService, times(1)).processIssueProof(any(IssueTicket.class));
    }

    @Test
    void testGetUserIssues() {
        IssueTicket t1 = new IssueTicket();
        t1.setId(1L);
        t1.setUserEmail("test@example.com");

        when(ticketRepository.findByUserEmailOrderByCreatedAtDesc("test@example.com")).thenReturn(List.of(t1));

        List<IssueTicket> issues = issueTicketService.getUserIssues("test@example.com");
        assertEquals(1, issues.size());
        assertEquals(1L, issues.get(0).getId());
    }

    @Test
    void testUpdateStatus() {
        IssueTicket ticket = new IssueTicket();
        ticket.setId(2L);
        ticket.setStatus(IssueStatus.OPEN);

        when(ticketRepository.findById(2L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(IssueTicket.class))).thenReturn(ticket);

        Optional<IssueTicket> updated = issueTicketService.updateStatus(2L, IssueStatus.RESOLVED, "test@example.com");
        assertTrue(updated.isPresent());
        assertEquals(IssueStatus.RESOLVED, updated.get().getStatus());
    }
}
