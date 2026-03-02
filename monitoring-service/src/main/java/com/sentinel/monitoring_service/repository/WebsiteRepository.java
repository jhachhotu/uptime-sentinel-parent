package com.sentinel.monitoring_service.repository;

import com.sentinel.monitoring_service.entity.Website;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebsiteRepository extends JpaRepository<Website, Long> {

    // Fetch websites based on check interval (seconds)
    List<Website> findByCheckInterval(int checkInterval);

    // Fetch websites owned by a specific user
    List<Website> findByOwnerId(String ownerId);

    // Fetch websites that have no owner (for auto-migration)
    @Query("SELECT w FROM Website w WHERE w.ownerId IS NULL")
    List<Website> findOrphanedWebsites();
}
