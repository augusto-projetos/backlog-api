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

    // ---------------------------------------------------------------
    // Critério de desbloqueio automático
    // ---------------------------------------------------------------

    /*
     * Tipo de critério usado pelo ConquistaService para verificar automaticamente.
     * Valores possíveis:
     *   TOTAL_ITENS          - total de itens cadastrados >= criterioValor
     *   TOTAL_CONCLUIDOS     - total de itens concluídos (qualquer tipo) >= criterioValor
     *   TOTAL_DROPADOS       - total de itens dropados >= criterioValor
     *   JOGOS_ZERADOS        - jogos zerados >= criterioValor
     *   FILMES_ASSISTIDOS    - filmes assistidos >= criterioValor
     *   SERIES_ASSISTIDAS    - séries assistidas >= criterioValor
     *   NOTA10_FILMES        - filmes com nota 10 >= criterioValor
     *   NOTA10_JOGOS         - jogos com nota 10 >= criterioValor
     *   NOTA10_TOTAL         - itens com nota 10 (qualquer tipo) >= criterioValor
     *   MANUAL               - concedida manualmente pelo admin (não verifica automaticamente)
     */
    @Column(length = 40)
    private String criterioTipo;

    /*
     * Valor numérico do critério (ex: 10 para "10 filmes assistidos").
     * Ignorado quando criterioTipo = MANUAL.
     */
    @Column
    private Integer criterioValor;
}
