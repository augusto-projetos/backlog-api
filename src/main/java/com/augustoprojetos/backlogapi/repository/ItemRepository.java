package com.augustoprojetos.backlogapi.repository;

import com.augustoprojetos.backlogapi.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

// <Item, Long> significa: "Vou cuidar da tabela Item, e o ID dela é Long"
public interface ItemRepository extends JpaRepository<Item, Long> {
    // Só isso! Acredite ou não, o Spring já criou o CRUD inteiro aqui dentro.
}