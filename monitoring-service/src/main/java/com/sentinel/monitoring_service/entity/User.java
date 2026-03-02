package com.sentinel.monitoring_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = true) // Can be null for Google Auth users
    private String password;

    @Column(nullable = false)
    private String provider; // "LOCAL" or "GOOGLE"

    @Column(nullable = true)
    private String providerId; // Google subject ID

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (provider == null) {
            provider = "LOCAL";
        }
    }
}
