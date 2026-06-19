package com.augustoprojetos.backlogapi.service;

import com.augustoprojetos.backlogapi.entity.EmailVerificationToken;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.repository.EmailVerificationTokenRepository;
import com.augustoprojetos.backlogapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class CleanupTask {

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    // Roda automaticamente a cada 1 hora (3600000 milissegundos)
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanUpExpiredTokens() {
        // Pede ao banco apenas os tokens que já passaram da data de agora
        List<EmailVerificationToken> expiredTokens = tokenRepository.findByExpiryDateBefore(LocalDateTime.now());

        for (EmailVerificationToken token : expiredTokens) {
            if (token.isExpired()) {
                User user = token.getUser();
                
                // Deleta o token expirado
                tokenRepository.delete(token);
                
                // Deleta o usuário inativo associado a ele
                if (!user.isEnabled()) {
                    userRepository.delete(user);
                    System.out.println("🧹 Limpeza automática: Conta fantasma apagada - " + user.getEmail());
                }
            }
        }
    }
}