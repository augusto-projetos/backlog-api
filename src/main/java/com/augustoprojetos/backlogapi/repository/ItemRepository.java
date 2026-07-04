package com.augustoprojetos.backlogapi.repository;

import com.augustoprojetos.backlogapi.entity.Item;
import com.augustoprojetos.backlogapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    // Busca apenas os itens deste usuário específico
    List<Item> findByUser(User user);

    void deleteByUser(User user);

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
}
