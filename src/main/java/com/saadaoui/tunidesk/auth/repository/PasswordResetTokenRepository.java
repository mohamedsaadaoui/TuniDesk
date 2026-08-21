package com.saadaoui.tunidesk.auth.repository;

import com.saadaoui.tunidesk.auth.entity.PasswordResetToken;
import com.saadaoui.tunidesk.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying
    void deleteByUser(User user);
}
