package com.augustoprojetos.backlogapi.repository;

import com.augustoprojetos.backlogapi.entity.Item;
import com.augustoprojetos.backlogapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    // Busca apenas os itens deste usuário específico
    List<Item> findByUser(User user);

}