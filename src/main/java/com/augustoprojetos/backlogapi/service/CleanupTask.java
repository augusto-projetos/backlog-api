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

    // Dias de retenção dos registros de auditoria
    private static final int DIAS_RETENCAO_AUDIT = 7;

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AtividadeLogService atividadeLogService;

    @Autowired
    private AuditLogService auditLogService;

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
                    // Garante que nenhum log órfão bloqueie a deleção
                    atividadeLogService.deletarLogsByUser(user);
                    userRepository.delete(user);
                    System.out.println("🧹 Limpeza automática: Conta fantasma apagada - " + user.getEmail());
                }
            }
        }
    }

    /*
     * Roda uma vez por dia (meia-noite) e remove todos os registros de auditoria
     * mais antigos que DIAS_RETENCAO_AUDIT (7 dias).
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanUpAuditLogs() {
        int removidos = auditLogService.limparLogsAntigos(DIAS_RETENCAO_AUDIT);
        if (removidos > 0) {
            System.out.println("🧹 [CleanupTask] Audit log: " + removidos
                + " registros removidos (retenção: " + DIAS_RETENCAO_AUDIT + " dias).");
        }
    }
}
