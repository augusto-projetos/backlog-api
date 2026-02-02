package com.augustoprojetos.backlogapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class ShareToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // O código que vai na URL (Ex: "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    @Column(nullable = false, unique = true)
    private String token;

    // Quando esse link deixa de funcionar?
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // Quantas vezes acessaram?
    private int visualizacoes = 0;

    // Quem é o dono desse link?
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // --- Construtores ---
    public ShareToken() {}

    public ShareToken(String token, User user, LocalDateTime expiresAt) {
        this.token = token;
        this.user = user;
        this.expiresAt = expiresAt;
    }

    // --- Getters e Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public int getVisualizacoes() { return visualizacoes; }
    public void setVisualizacoes(int visualizacoes) { this.visualizacoes = visualizacoes; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    // Metodo auxiliar para saber se já venceu
    public boolean isExpirado() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}