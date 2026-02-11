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
}