package com.saadaoui.tunidesk.auth.service.impl;

import com.saadaoui.tunidesk.audit.service.AuditLogService;
import com.saadaoui.tunidesk.auth.dto.ForgotPasswordRequest;
import com.saadaoui.tunidesk.auth.dto.ResetPasswordRequest;
import com.saadaoui.tunidesk.auth.entity.PasswordResetToken;
import com.saadaoui.tunidesk.auth.entity.Role;
import com.saadaoui.tunidesk.auth.entity.User;
import com.saadaoui.tunidesk.auth.repository.PasswordResetTokenRepository;
import com.saadaoui.tunidesk.auth.repository.RoleRepository;
import com.saadaoui.tunidesk.auth.repository.UserRepository;
import com.saadaoui.tunidesk.auth.security.JwtService;
import com.saadaoui.tunidesk.email.service.EmailService;
import com.saadaoui.tunidesk.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private EmailService emailService;

    private AuthenticationServiceImpl service;

    private User user;

    @BeforeEach
    void setUp() {

        service = new AuthenticationServiceImpl(
                userRepository, roleRepository, passwordResetTokenRepository,
                passwordEncoder, jwtService, authenticationManager,
                auditLogService, emailService);

        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:5173");

        user = User.builder()
                .id(UUID.randomUUID())
                .firstName("Amine")
                .lastName("Ben Salah")
                .email("amine@test.com")
                .password("encoded")
                .enabled(true)
                .roles(new java.util.HashSet<>(java.util.Set.of(Role.builder().name("USER").build())))
                .build();
    }

    @Test
    void forgotPasswordCreatesTokenAndSendsEmail() {

        when(userRepository.findByEmail("amine@test.com")).thenReturn(Optional.of(user));

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("amine@test.com");

        service.forgotPassword(request);

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).deleteByUser(user);
        verify(passwordResetTokenRepository).save(captor.capture());

        PasswordResetToken saved = captor.getValue();
        assertThat(saved.getTokenHash()).hasSize(64);
        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now().plusMinutes(55));

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq(user), linkCaptor.capture());
        assertThat(linkCaptor.getValue()).startsWith("http://localhost:5173/reset-password?token=");
    }

    @Test
    void forgotPasswordIsSilentForUnknownEmail() {

        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("ghost@test.com");

        service.forgotPassword(request);

        verifyNoInteractions(passwordResetTokenRepository, emailService);
    }

    @Test
    void forgotPasswordSkipsDisabledUsers() {

        user.setEnabled(false);
        when(userRepository.findByEmail("amine@test.com")).thenReturn(Optional.of(user));

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("amine@test.com");

        service.forgotPassword(request);

        verifyNoInteractions(passwordResetTokenRepository, emailService);
    }

    @Test
    void resetPasswordUpdatesPasswordAndMarksTokenUsed() {

        String rawToken = "abc123";
        PasswordResetToken token = PasswordResetToken.builder()
                .tokenHash(sha256(rawToken))
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        when(passwordResetTokenRepository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewPass@123")).thenReturn("encoded-new");

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(rawToken);
        request.setNewPassword("NewPass@123");

        service.resetPassword(request);

        assertThat(user.getPassword()).isEqualTo("encoded-new");
        assertThat(token.isUsed()).isTrue();
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(token);
        verify(auditLogService).record(anyString(), anyString(), eq(user.getId()), anyString(), anyString());
    }

    @Test
    void resetPasswordRejectsUnknownToken() {

        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("bad");
        request.setNewPassword("NewPass@123");

        assertThatThrownBy(() -> service.resetPassword(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPasswordRejectsExpiredToken() {

        String rawToken = "abc123";
        PasswordResetToken token = PasswordResetToken.builder()
                .tokenHash(sha256(rawToken))
                .user(user)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(passwordResetTokenRepository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(token));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(rawToken);
        request.setNewPassword("NewPass@123");

        assertThatThrownBy(() -> service.resetPassword(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPasswordRejectsAlreadyUsedToken() {

        String rawToken = "abc123";
        PasswordResetToken token = PasswordResetToken.builder()
                .tokenHash(sha256(rawToken))
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .usedAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(passwordResetTokenRepository.findByTokenHash(sha256(rawToken))).thenReturn(Optional.of(token));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(rawToken);
        request.setNewPassword("NewPass@123");

        assertThatThrownBy(() -> service.resetPassword(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already been used");

        verify(userRepository, never()).save(any());
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
