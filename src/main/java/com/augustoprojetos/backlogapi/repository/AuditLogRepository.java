package com.augustoprojetos.backlogapi.repository;

import com.augustoprojetos.backlogapi.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Todos os logs, paginados
    Page<AuditLog> findAllByOrderByCriadoEmDesc(Pageable pageable);

    // Filtro por ação
    Page<AuditLog> findByAcaoOrderByCriadoEmDesc(String acao, Pageable pageable);

    // Filtro por alvo (nome de usuário, conquista, etc.)
    @Query("SELECT a FROM AuditLog a WHERE LOWER(a.alvoNome) LIKE LOWER(CONCAT('%', :nome, '%')) ORDER BY a.criadoEm DESC")
    Page<AuditLog> findByAlvoNomeContainingIgnoreCase(@Param("nome") String nome, Pageable pageable);

    // Filtro combinado: ação + alvo
    @Query("SELECT a FROM AuditLog a WHERE (:acao IS NULL OR a.acao = :acao) AND (:alvoNome IS NULL OR LOWER(a.alvoNome) LIKE LOWER(CONCAT('%', :alvoNome, '%'))) ORDER BY a.criadoEm DESC")
    Page<AuditLog> findWithFilters(
            @Param("acao") String acao,
            @Param("alvoNome") String alvoNome,
            Pageable pageable);

    // Para limpeza automática: deleta todos os registros mais antigos que X dias
    @Modifying
    @Query("DELETE FROM AuditLog a WHERE a.criadoEm < :limite")
    int deleteBycriadoEmBefore(@Param("limite") LocalDateTime limite);

    // Lista todas as ações distintas (para popular o dropdown de filtro)
    @Query("SELECT DISTINCT a.acao FROM AuditLog a ORDER BY a.acao")
    List<String> findAcoesDistintas();

    // Contagem total (para o badge no cabeçalho da aba)
    long count();
}
