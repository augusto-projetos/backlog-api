package com.augustoprojetos.backlogapi.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_verification_tokens")
@Data
@NoArgsConstructor
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    // Relacionamento 1 para 1 com o User.
    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    // Construtor prático para gerar o token e a data de expiração automaticamente
    public EmailVerificationToken(User user) {
        this.user = user;
        this.token = UUID.randomUUID().toString();
        // Expira em 24 horas a partir do momento da criação
        this.expiryDate = LocalDateTime.now().plusHours(24);
    }

    // Método auxiliar para verificar se já expirou
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
}
