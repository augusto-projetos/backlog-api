package com.augustoprojetos.backlogapi.repository;

import com.augustoprojetos.backlogapi.entity.Item;
import com.augustoprojetos.backlogapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    // Busca apenas os itens deste usuário específico
    List<Item> findByUser(User user);

    // Localiza um cadastro já feito com a mesma chave de idempotência (evita item duplicado)
    Optional<Item> findByUserAndClientRequestId(User user, String clientRequestId);

    // Ordenada alfabeticamente pelo título
    @Query("SELECT i FROM Item i WHERE i.user = :user ORDER BY LOWER(i.titulo) ASC")
    List<Item> findByUserOrderByTituloAsc(@Param("user") User user);

    void deleteByUser(User user);

    // Filmes (qualquer caixa em "tipo") ainda sem duração: o filtro roda no banco, não em memória
    List<Item> findByUserAndTipoIgnoreCaseAndDuracaoMinutosIsNull(User user, String tipo);

    /*
     * Todos os contadores do sistema de conquistas em uma query.
     *
     * Colunas, nesta ordem (ver EstatisticasConquistaDTO.de):
     *  0 total de itens           1 concluídos              2 dropados
     *  3 jogos zerados            4 filmes assistidos       5 séries assistidas
     *  6 nota 10 (total)          7 filmes nota 10          8 jogos nota 10
     *  9 itens com capa TMDB     10 min. de filmes concluídos
     * 11 min. jogados (total)    12 maior tempo em 1 jogo  13 min. de séries concluídas
     * 14 episódios (total)       15 maior nº de episódios em 1 série
     *
     * SUM/MAX voltam null quando o usuário não tem itens; o DTO converte para 0.
     */
    @Query("""
            SELECT COUNT(i),
                   SUM(CASE WHEN i.status IN :concluidos THEN 1 ELSE 0 END),
                   SUM(CASE WHEN i.status IN :dropados THEN 1 ELSE 0 END),
                   SUM(CASE WHEN i.tipo = :tipoJogo AND i.status IN :concluidos THEN 1 ELSE 0 END),
                   SUM(CASE WHEN i.tipo = :tipoFilme AND i.status IN :concluidos THEN 1 ELSE 0 END),
                   SUM(CASE WHEN i.tipo = :tipoSerie AND i.status IN :concluidos THEN 1 ELSE 0 END),
                   SUM(CASE WHEN i.nota = 10.0 THEN 1 ELSE 0 END),
                   SUM(CASE WHEN i.tipo = :tipoFilme AND i.nota = 10.0 THEN 1 ELSE 0 END),
                   SUM(CASE WHEN i.tipo = :tipoJogo AND i.nota = 10.0 THEN 1 ELSE 0 END),
                   SUM(CASE WHEN i.imagemUrl LIKE '%themoviedb.org%' THEN 1 ELSE 0 END),
                   SUM(CASE WHEN i.tipo = :tipoFilme AND i.status IN :concluidos AND i.duracaoMinutos IS NOT NULL
                            THEN i.duracaoMinutos ELSE 0 END),
                   SUM(CASE WHEN i.tipo = :tipoJogo AND i.minutosJogados IS NOT NULL
                            THEN i.minutosJogados ELSE 0 END),
                   MAX(CASE WHEN i.tipo = :tipoJogo AND i.minutosJogados IS NOT NULL
                            THEN i.minutosJogados ELSE 0 END),
                   SUM(CASE WHEN i.tipo = :tipoSerie AND i.status IN :concluidos AND i.duracaoTotalMinutos IS NOT NULL
                            THEN i.duracaoTotalMinutos ELSE 0 END),
                   SUM(CASE WHEN i.tipo = :tipoSerie AND i.episodioAtual IS NOT NULL
                            THEN i.episodioAtual ELSE 0 END),
                   MAX(CASE WHEN i.tipo = :tipoSerie AND i.episodioAtual IS NOT NULL
                            THEN i.episodioAtual ELSE 0 END)
            FROM Item i
            WHERE i.user.id = :userId
            """)
    List<Object[]> agregarEstatisticasConquistas(@Param("userId") Long userId,
                                                 @Param("concluidos") List<String> concluidos,
                                                 @Param("dropados") List<String> dropados,
                                                 @Param("tipoJogo") String tipoJogo,
                                                 @Param("tipoFilme") String tipoFilme,
                                                 @Param("tipoSerie") String tipoSerie);

    // 1. Conta quantos itens existem por TIPO (Ex: Jogo: 10, Filme: 5)
    @Query("SELECT i.tipo, COUNT(i) FROM Item i WHERE i.user.id = :userId GROUP BY i.tipo")
    List<Object[]> countItensPorTipo(@Param("userId") Long userId);

    // 2. Conta quantos itens existem por STATUS (Ex: Zerado: 8, Backlog: 20)
    @Query("SELECT i.status, COUNT(i) FROM Item i WHERE i.user.id = :userId GROUP BY i.status")
    List<Object[]> countItensPorStatus(@Param("userId") Long userId);

    // 3. Distribuição de Notas (Ex: Nota 10: 5 itens, Nota 9.5: 2 itens)
    @Query("SELECT i.nota, COUNT(i) FROM Item i WHERE i.user.id = :userId AND i.nota > 0 GROUP BY i.nota ORDER BY i.nota DESC")
    List<Object[]> countItensPorNota(@Param("userId") Long userId);

    // --- Queries para o sistema de conquistas ---

    // Conta itens de um usuário com tipo e nota específicos
    long countByUserAndTipoAndNota(User user, String tipo, Double nota);

    // Conta itens de um usuário com tipo e status dentro de uma lista
    long countByUserAndTipoAndStatusIn(User user, String tipo, List<String> statusList);

    // --- Queries de tempo (horas) para o sistema de conquistas ---

    // Soma os minutos de duração dos filmes concluídos (assistidos) de um usuário
    @Query("SELECT COALESCE(SUM(i.duracaoMinutos), 0) FROM Item i " +
           "WHERE i.user = :user AND i.tipo = :tipo AND i.status IN :statusList AND i.duracaoMinutos IS NOT NULL")
    long sumDuracaoMinutosByUserAndTipoAndStatusIn(@Param("user") User user,
                                                    @Param("tipo") String tipo,
                                                    @Param("statusList") List<String> statusList);

    // Soma os minutos jogados em TODOS os jogos cadastrados por um usuário (qualquer status)
    @Query("SELECT COALESCE(SUM(i.minutosJogados), 0) FROM Item i " +
           "WHERE i.user = :user AND i.tipo = :tipo AND i.minutosJogados IS NOT NULL")
    long sumMinutosJogadosByUserAndTipo(@Param("user") User user, @Param("tipo") String tipo);

    // Maior quantidade de minutos jogados em um ÚNICO jogo cadastrado por um usuário
    @Query("SELECT COALESCE(MAX(i.minutosJogados), 0) FROM Item i " +
           "WHERE i.user = :user AND i.tipo = :tipo AND i.minutosJogados IS NOT NULL")
    long maxMinutosJogadosByUserAndTipo(@Param("user") User user, @Param("tipo") String tipo);

    // Soma os minutos de duração TOTAL das séries concluídas (assistidas) de um usuário
    @Query("SELECT COALESCE(SUM(i.duracaoTotalMinutos), 0) FROM Item i " +
           "WHERE i.user = :user AND i.tipo = :tipo AND i.status IN :statusList AND i.duracaoTotalMinutos IS NOT NULL")
    long sumDuracaoTotalMinutosByUserAndTipoAndStatusIn(@Param("user") User user,
                                                         @Param("tipo") String tipo,
                                                         @Param("statusList") List<String> statusList);

    // Soma o episódio atual de TODAS as séries cadastradas por um usuário (qualquer status),
    // usado como aproximação do total de episódios já vistos no acervo.
    @Query("SELECT COALESCE(SUM(i.episodioAtual), 0) FROM Item i " +
           "WHERE i.user = :user AND i.tipo = :tipo AND i.episodioAtual IS NOT NULL")
    long sumEpisodiosByUserAndTipo(@Param("user") User user, @Param("tipo") String tipo);

    // Maior episódio atual entre as séries cadastradas por um usuário (maratona de uma série só)
    @Query("SELECT COALESCE(MAX(i.episodioAtual), 0) FROM Item i " +
           "WHERE i.user = :user AND i.tipo = :tipo AND i.episodioAtual IS NOT NULL")
    long maxEpisodiosByUserAndTipo(@Param("user") User user, @Param("tipo") String tipo);
}
