package com.augustoprojetos.backlogapi.repository;

import com.augustoprojetos.backlogapi.entity.PasswordResetToken;
import com.augustoprojetos.backlogapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByUser(User user);
}