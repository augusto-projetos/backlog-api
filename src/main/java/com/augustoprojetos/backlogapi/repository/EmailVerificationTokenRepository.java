package com.augustoprojetos.backlogapi.repository;

import com.augustoprojetos.backlogapi.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    
    // Busca o token pela string UUID gerada
    Optional<EmailVerificationToken> findByToken(String token);
    
    // Útil caso a gente precise deletar tokens antigos de um usuário específico
    Optional<EmailVerificationToken> findByUser_Id(Long userId);
}