package com.augustoprojetos.backlogapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_config")
public class SystemConfig {

    @Id
    @Column(name = "chave", length = 100)
    private String chave;

    @Column(name = "valor", length = 500, nullable = false)
    private String valor;

    @Column(name = "descricao", length = 500)
    private String descricao;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    public SystemConfig() {}

    public SystemConfig(String chave, String valor, String descricao) {
        this.chave = chave;
        this.valor = valor;
        this.descricao = descricao;
        this.atualizadoEm = LocalDateTime.now();
    }

    // Getters e Setters
    public String getChave()                        { return chave; }
    public void   setChave(String chave)            { this.chave = chave; }
    public String getValor()                        { return valor; }
    public void   setValor(String valor)            { this.valor = valor; }
    public String getDescricao()                    { return descricao; }
    public void   setDescricao(String descricao)    { this.descricao = descricao; }
    public LocalDateTime getAtualizadoEm()          { return atualizadoEm; }
    public void   setAtualizadoEm(LocalDateTime t)  { this.atualizadoEm = t; }

    public boolean isAtivo() {
        return "true".equalsIgnoreCase(valor);
    }
}