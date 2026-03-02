package com.sentinel.monitoring_service.repository;

import com.sentinel.monitoring_service.entity.IncidentComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentCommentRepository extends JpaRepository<IncidentComment, Long> {

    List<IncidentComment> findByWebsiteIdOrderByCreatedAtDesc(Long websiteId);
}
