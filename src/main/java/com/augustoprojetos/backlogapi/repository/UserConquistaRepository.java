package com.augustoprojetos.backlogapi.repository;

import com.augustoprojetos.backlogapi.entity.Conquista;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.entity.UserConquista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserConquistaRepository extends JpaRepository<UserConquista, Long> {

    List<UserConquista> findByUser(User user);

    boolean existsByUserAndConquista_Chave(User user, String chave);

    // Soma total de XP ganho pelo usuário
    @Query("SELECT COALESCE(SUM(uc.conquista.xp), 0) FROM UserConquista uc WHERE uc.user.id = :userId")
    int sumXpByUserId(@Param("userId") Long userId);

    void deleteByUser(User user);
    void deleteByConquista(Conquista conquista);
}