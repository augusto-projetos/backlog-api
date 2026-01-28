package com.augustoprojetos.backlogapi.controller;

import com.augustoprojetos.backlogapi.entity.Item;
import com.augustoprojetos.backlogapi.repository.ItemRepository;
import jakarta.validation.Valid;
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
    // O @Valid diz: "Spring, checa se esse item cumpre as regras antes de entrar aqui"
    public Item cadastrar(@RequestBody @Valid Item item) {
        return itemRepository.save(item);
    }

    // 2. Método para LISTAR TUDO (GET)
    @GetMapping
    public List<Item> listar() {
        return itemRepository.findAll(); // O findAll também já vem pronto!
    }

    // 3. Método para EDITAR (PUT)
    // URL vai ser: http://localhost:8080/itens/1 (onde 1 é o ID)
    @PutMapping("/{id}")
    public Item atualizar(@PathVariable Long id, @RequestBody @Valid Item itemAtualizado) {
        itemAtualizado.setId(id);
        return itemRepository.save(itemAtualizado);
    }

    // 4. Método para DELETAR (DELETE)
    // URL vai ser: http://localhost:8080/itens/1
    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        itemRepository.deleteById(id);
    }

    // 5. Método para BUSCAR UM SÓ (GET com ID)
    // Serve para preencher o formulário de edição
    @GetMapping("/{id}")
    public Item buscarPorId(@PathVariable Long id) {
        return itemRepository.findById(id).orElse(null);
    }

    // Rota da Capa (Landing Page)
    @GetMapping("/")
    public String index() {
        return "index"; // Retorna o index.html (capa)
    }

    // Rota da Lista Privada
    @GetMapping("/home")
    public String home() {
        return "home"; // Retorna o home.html (lista)
    }
}