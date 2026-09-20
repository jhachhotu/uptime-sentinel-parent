package com.sentinel.monitoring_service.repository;

import com.sentinel.monitoring_service.entity.IssueStatus;
import com.sentinel.monitoring_service.entity.IssueTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueTicketRepository extends JpaRepository<IssueTicket, Long> {

    List<IssueTicket> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    List<IssueTicket> findByWebsiteIdOrderByCreatedAtDesc(Long websiteId);

    List<IssueTicket> findByStatusOrderByCreatedAtDesc(IssueStatus status);

    long countByStatus(IssueStatus status);
}
