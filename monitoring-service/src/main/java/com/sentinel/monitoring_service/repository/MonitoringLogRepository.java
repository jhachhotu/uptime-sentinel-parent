package com.sentinel.monitoring_service.repository;

import com.sentinel.monitoring_service.entity.MonitoringLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MonitoringLogRepository extends JpaRepository<MonitoringLog, Long> {

    // Find logs for a specific website within a time range
    List<MonitoringLog> findByWebsiteIdAndTimestampAfter(Long websiteId, LocalDateTime since);

    // Find recent logs for incident timeline (ordered newest first)
    List<MonitoringLog> findByWebsiteIdAndTimestampAfterOrderByTimestampDesc(Long websiteId, LocalDateTime since);

    // Calculate Average Response Time for the "Response time summary"
    @Query("SELECT AVG(l.responseTime) FROM MonitoringLog l WHERE l.website.id = :websiteId AND l.timestamp >= :since")
    Double getAverageResponseTime(@Param("websiteId") Long websiteId, @Param("since") LocalDateTime since);

    // Count successful pings to calculate Uptime %
    @Query("SELECT COUNT(l) FROM MonitoringLog l WHERE l.website.id = :websiteId AND l.status = 'UP' AND l.timestamp >= :since")
    long countUpStatus(@Param("websiteId") Long websiteId, @Param("since") LocalDateTime since);

    long countByWebsiteIdAndTimestampAfter(Long id, LocalDateTime oneMonthAgo);

    // Delete all logs for a website (cascade cleanup)
    @Modifying
    @Transactional
    void deleteByWebsiteId(Long websiteId);
}