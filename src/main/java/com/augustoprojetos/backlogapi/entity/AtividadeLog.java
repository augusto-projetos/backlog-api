package com.augustoprojetos.backlogapi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Entity
@Table(name = "atividade_log")
@Data
@NoArgsConstructor
public class AtividadeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false, length = 50)
    private String tipo;

    // Texto principal exibido na timeline. Ex: "Adicionou Cruella ao backlog"
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    // Informação extra opcional. Ex: status anterior → novo, ou nota
    @Column(length = 500)
    private String detalhe;

    // Título do item relacionado, para exibição rápida sem JOIN
    @Column(length = 255)
    private String itemTitulo;

    // Identifica eventos que aconteceram juntos na mesma ação do usuário.
    @Column(name = "grupo_id", length = 36)
    private String grupoId;

    @Column(nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    // --- Factory helpers ---

    public static AtividadeLog of(User user, String tipo, String descricao) {
        AtividadeLog log = new AtividadeLog();
        log.setUser(user);
        log.setTipo(tipo);
        log.setDescricao(descricao);
        return log;
    }

    public static AtividadeLog of(User user, String tipo, String descricao,
                                  String detalhe, String itemTitulo) {
        AtividadeLog log = of(user, tipo, descricao);
        log.setDetalhe(detalhe);
        log.setItemTitulo(itemTitulo);
        return log;
    }
}
