package com.sentinel.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
// --- FIXED IMPORTS ---
import org.springframework.security.web.server.header.CrossOriginOpenerPolicyServerHttpHeadersWriter.CrossOriginOpenerPolicy;
// ---------------------
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

        @Bean
        public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
                return http
                                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                                // 1. FIXED: Correct path for Reactive COOP policy
                                .headers(headers -> headers
                                                .crossOriginOpenerPolicy(coop -> coop
                                                                .policy(CrossOriginOpenerPolicy.SAME_ORIGIN_ALLOW_POPUPS)))

                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                                .authorizeExchange(exchanges -> exchanges
                                                .anyExchange().permitAll())
                                .build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(Arrays.asList(
                        "https://client-uptime-frontend.vercel.app",
                        "http://localhost:5173",
                        "http://localhost:8080",
                        "http://localhost:8082"
                ));
                config.setAllowedOriginPatterns(Arrays.asList("https://*.vercel.app"));
                config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(Arrays.asList("*"));
                config.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }
}