package com.augustoprojetos.backlogapi.repository;

import com.augustoprojetos.backlogapi.entity.AtividadeLog;
import com.augustoprojetos.backlogapi.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AtividadeLogRepository extends JpaRepository<AtividadeLog, Long> {

    // Todos os logs de um usuário, do mais recente ao mais antigo
    List<AtividadeLog> findByUserOrderByCriadoEmDesc(User user);

    // Paginado — para o admin navegar entre muitos logs
    Page<AtividadeLog> findByUserOrderByCriadoEmDesc(User user, Pageable pageable);

    // Admin: todos os logs, do mais recente ao mais antigo
    Page<AtividadeLog> findAllByOrderByCriadoEmDesc(Pageable pageable);

    // Admin: todos os logs de um usuário específico (por ID)
    @Query("SELECT a FROM AtividadeLog a WHERE a.user.id = :userId ORDER BY a.criadoEm DESC")
    List<AtividadeLog> findByUserIdOrderByCriadoEmDesc(@Param("userId") Long userId);

    // Conta total de logs de um usuário
    long countByUser(User user);

    // Remove todos os logs de um usuário (chamado ao deletar conta)
    void deleteByUser(User user);
}
