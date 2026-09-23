package com.sentinel.monitoring_service.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.sentinel.monitoring_service.dto.AuthRequest;
import com.sentinel.monitoring_service.dto.AuthResponse;
import com.sentinel.monitoring_service.dto.GoogleLoginRequest;
import com.sentinel.monitoring_service.dto.RegisterRequest;
import com.sentinel.monitoring_service.entity.User;
import com.sentinel.monitoring_service.repository.UserRepository;
import com.sentinel.monitoring_service.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Value("${google.client-id:dummyClientId}")
    private String googleClientId;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .provider("LOCAL")
                .build();

        userRepository.save(user);
        String jwtToken = jwtUtil.generateToken(user.getEmail());

        return AuthResponse.builder()
                .token(jwtToken)
                .email(user.getEmail())
                .build();
    }

    public AuthResponse authenticate(AuthRequest request) {
        // Check if the user exists and is a Google-only account
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isPresent()) {
            User existingUser = userOpt.get();
            if ("GOOGLE".equals(existingUser.getProvider()) && existingUser.getPassword() == null) {
                throw new RuntimeException(
                        "This account was created with Google Sign-In. Please use 'Continue with Google' to log in.");
            }
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        User user = userOpt
                .orElseThrow(() -> new RuntimeException("User not found"));

        String jwtToken = jwtUtil.generateToken(user.getEmail());

        return AuthResponse.builder()
                .token(jwtToken)
                .email(user.getEmail())
                .build();
    }

    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),
                    new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(request.getIdToken());
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String subjectId = payload.getSubject();

                Optional<User> userOptional = userRepository.findByEmail(email);
                User user;

                if (userOptional.isPresent()) {
                    user = userOptional.get();
                    // If user was originally LOCAL, update provider info for linking
                    if ("LOCAL".equals(user.getProvider()) && user.getProviderId() == null) {
                        user.setProviderId(subjectId);
                    }
                } else {
                    user = User.builder()
                            .email(email)
                            .provider("GOOGLE")
                            .providerId(subjectId)
                            .build();
                    userRepository.save(user);
                }

                String jwtToken = jwtUtil.generateToken(user.getEmail());

                return AuthResponse.builder()
                        .token(jwtToken)
                        .email(user.getEmail())
                        .build();

            } else {
                throw new RuntimeException("Invalid Google ID token.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error verifying Google token: " + e.getMessage(), e);
        }
    }
}
