package com.augustoprojetos.backlogapi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "conquistas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Conquista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Chave única para referenciar a conquista no código (ex: "GUERREIRO_FDSEM")
    @Column(unique = true, nullable = false, length = 60)
    private String chave;

    // Nome exibido ao usuário
    @Column(nullable = false)
    private String nome;

    // Descrição da conquista exibida no perfil/toast
    @Column(nullable = false, length = 300)
    private String descricao;

    // Emoji ou ícone da conquista
    @Column(nullable = false, length = 10)
    private String icone;

    // Quantidade de XP concedida ao desbloquear
    @Column(nullable = false)
    private int xp;
}
