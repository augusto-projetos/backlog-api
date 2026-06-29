package com.augustoprojetos.backlogapi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Registro de ação crítica do sistema para monitoramento do administrador.
 * Diferente do AtividadeLog (timeline do usuário), este registra eventos
 * sensíveis como exclusões de contas, alterações de senha, criação de conquistas, etc.
 */
@Entity
@Table(name = "audit_log")
@Data
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código da ação. Ex: CONTA_DELETADA, SENHA_REDEFINIDA, CONQUISTA_CRIADA */
    @Column(nullable = false, length = 60)
    private String acao;

    /** Texto legível descrevendo o evento. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    /** Informação extra opcional (JSON, diff, motivo, etc.) */
    @Column(length = 1000)
    private String detalhe;

    /** Tipo do objeto afetado: USUARIO, CONQUISTA, ITEM */
    @Column(length = 50)
    private String alvoTipo;

    /** ID do objeto afetado (para correlação). */
    private Long alvoId;

    /** Nome/login do objeto afetado (guardado para sobreviver à deleção). */
    @Column(length = 255)
    private String alvoNome;

    /** IP do cliente que originou a ação (extraído do request). */
    @Column(length = 50)
    private String ip;

    @Column(nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    // --- Factory helpers ---

    public static AuditLog of(String acao, String descricao) {
        AuditLog log = new AuditLog();
        log.setAcao(acao);
        log.setDescricao(descricao);
        return log;
    }

    public static AuditLog of(String acao, String descricao,
                               String alvoTipo, Long alvoId, String alvoNome) {
        AuditLog log = of(acao, descricao);
        log.setAlvoTipo(alvoTipo);
        log.setAlvoId(alvoId);
        log.setAlvoNome(alvoNome);
        return log;
    }

    public static AuditLog of(String acao, String descricao,
                               String alvoTipo, Long alvoId, String alvoNome,
                               String detalhe, String ip) {
        AuditLog log = of(acao, descricao, alvoTipo, alvoId, alvoNome);
        log.setDetalhe(detalhe);
        log.setIp(ip);
        return log;
    }
}
