package com.augustoprojetos.backlogapi.controller;

import com.augustoprojetos.backlogapi.entity.Item;
import com.augustoprojetos.backlogapi.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // Diz: "Sou um controlador REST"
@RequestMapping("/itens") // Diz: "Minha URL base é http://localhost:8080/itens"
public class ItemController {

    @Autowired // Diz: "Spring, injeta o repositório aqui pra mim (não preciso dar new)"
    private ItemRepository itemRepository;

    // 1. Método para CADASTRAR (POST)
    @PostMapping
    public Item cadastrar(@RequestBody Item item) {
        return itemRepository.save(item); // O save já vem pronto do JpaRepository!
    }

    // 2. Método para LISTAR TUDO (GET)
    @GetMapping
    public List<Item> listar() {
        return itemRepository.findAll(); // O findAll também já vem pronto!
    }

    // 3. Método para EDITAR (PUT)
    // URL vai ser: http://localhost:8080/itens/1 (onde 1 é o ID)
    @PutMapping("/{id}")
    public Item atualizar(@PathVariable Long id, @RequestBody Item itemAtualizado) {
        itemAtualizado.setId(id); // Força o ID do objeto ser o mesmo da URL
        return itemRepository.save(itemAtualizado); // O save também serve para atualizar!
    }

    // 4. Método para DELETAR (DELETE)
    // URL vai ser: http://localhost:8080/itens/1
    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        itemRepository.deleteById(id);
    }
}