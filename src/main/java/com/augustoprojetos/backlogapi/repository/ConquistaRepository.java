package com.augustoprojetos.backlogapi.repository;

import com.augustoprojetos.backlogapi.entity.Conquista;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ConquistaRepository extends JpaRepository<Conquista, Long> {
    Optional<Conquista> findByChave(String chave);
}
