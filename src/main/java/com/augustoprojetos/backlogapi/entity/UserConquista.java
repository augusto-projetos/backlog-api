package com.augustoprojetos.backlogapi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

// Tabela intermediária: registra QUAIS conquistas UM usuário desbloqueou e QUANDO.
@Entity
@Table(
    name = "user_conquistas",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "conquista_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserConquista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "conquista_id", nullable = false)
    private Conquista conquista;

    @Column(nullable = false)
    private LocalDateTime desbloqueadaEm;

    public UserConquista(User user, Conquista conquista) {
        this.user = user;
        this.conquista = conquista;
        this.desbloqueadaEm = LocalDateTime.now();
    }
}
