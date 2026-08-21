package com.saadaoui.tunidesk.auth.service.impl;

import com.saadaoui.tunidesk.audit.AuditConstants;
import com.saadaoui.tunidesk.audit.service.AuditLogService;
import com.saadaoui.tunidesk.auth.dto.AuthResponse;
import com.saadaoui.tunidesk.auth.dto.ForgotPasswordRequest;
import com.saadaoui.tunidesk.auth.dto.LoginRequest;
import com.saadaoui.tunidesk.auth.dto.RegisterRequest;
import com.saadaoui.tunidesk.auth.dto.ResetPasswordRequest;
import com.saadaoui.tunidesk.auth.entity.PasswordResetToken;
import com.saadaoui.tunidesk.auth.entity.Role;
import com.saadaoui.tunidesk.auth.entity.User;
import com.saadaoui.tunidesk.auth.repository.PasswordResetTokenRepository;
import com.saadaoui.tunidesk.auth.repository.RoleRepository;
import com.saadaoui.tunidesk.auth.repository.UserRepository;
import com.saadaoui.tunidesk.auth.security.JwtService;
import com.saadaoui.tunidesk.auth.service.AuthenticationService;
import com.saadaoui.tunidesk.email.service.EmailService;
import com.saadaoui.tunidesk.exception.DuplicateResourceException;
import com.saadaoui.tunidesk.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final int RESET_TOKEN_BYTES = 32;

    private static final long RESET_TOKEN_VALIDITY_MINUTES = 60;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuditLogService auditLogService;
    private final EmailService emailService;

    @Value("${application.email.base-url:http://localhost:5173}")
    private String baseUrl;

    @Override
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        Role role = roleRepository.findByName("USER")
                .orElseThrow(() -> new ResourceNotFoundException("Default role not found"));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .roles(Set.of(role))
                .build();

        userRepository.save(user);

        auditLogService.record(AuditConstants.ACTION_REGISTER, AuditConstants.ENTITY_USER,
                user.getId(), "Registered account \"" + user.getEmail() + "\"",
                user.getEmail());

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .roles(roleNames(user))
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        auditLogService.record(AuditConstants.ACTION_LOGIN, AuditConstants.ENTITY_USER,
                user.getId(), "Logged in as \"" + user.getEmail() + "\"",
                user.getEmail());

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .roles(roleNames(user))
                .build();
    }

    private List<String> roleNames(User user) {

        return user.getRoles().stream().map(Role::getName).toList();
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {

        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            if (!Boolean.TRUE.equals(user.getEnabled())) {
                return;
            }

            passwordResetTokenRepository.deleteByUser(user);

            String rawToken = generateToken();
            PasswordResetToken token = PasswordResetToken.builder()
                    .tokenHash(sha256(rawToken))
                    .user(user)
                    .expiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_VALIDITY_MINUTES))
                    .build();
            passwordResetTokenRepository.save(token);

            String resetLink = baseUrl + "/reset-password?token=" + rawToken;
            emailService.sendPasswordResetEmail(user, resetLink);

            auditLogService.record(AuditConstants.ACTION_PASSWORD_RESET_REQUESTED, AuditConstants.ENTITY_USER,
                    user.getId(), "Requested a password reset for \"" + user.getEmail() + "\"",
                    user.getEmail());

            log.info("Password reset requested for {}", user.getEmail());
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {

        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(sha256(request.getToken()))
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired reset token"));

        if (token.isUsed()) {
            throw new IllegalArgumentException("This reset link has already been used");
        }

        if (token.isExpired(LocalDateTime.now())) {
            throw new IllegalArgumentException("This reset link has expired. Please request a new one");
        }

        User user = token.getUser();

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new IllegalArgumentException("This account is disabled");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        token.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(token);

        auditLogService.record(AuditConstants.ACTION_PASSWORD_RESET, AuditConstants.ENTITY_USER,
                user.getId(), "Password was reset via email link", user.getEmail());

        log.info("Password reset completed for {}", user.getEmail());
    }

    private String generateToken() {

        byte[] bytes = new byte[RESET_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String sha256(String value) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }
}
