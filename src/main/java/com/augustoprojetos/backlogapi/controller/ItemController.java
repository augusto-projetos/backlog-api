package com.augustoprojetos.backlogapi.controller;

import com.augustoprojetos.backlogapi.entity.Item;
import com.augustoprojetos.backlogapi.repository.ItemRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class ItemController {

    @Autowired
    private ItemRepository itemRepository;

    // --- PARTE 1: PÁGINAS (VIEW - Retorna HTML) ---

    // Rota da Capa (Landing Page)
    @GetMapping("/")
    public String index() {
        return "index"; // Vai procurar index.html na pasta templates
    }

    // Rota da Lista Privada
    @GetMapping("/home")
    public String home() {
        return "home"; // Vai procurar home.html na pasta templates
    }

    // --- PARTE 2: API (JSON - Retorna Dados para o JavaScript) ---

    // 1. CADASTRAR
    @PostMapping("/itens")
    @ResponseBody // Diz: "Não procure HTML, retorne o JSON do item criado"
    public Item cadastrar(@RequestBody @Valid Item item) {
        return itemRepository.save(item);
    }

    // 2. LISTAR TUDO
    @GetMapping("/itens")
    @ResponseBody // Diz: "Retorne a lista em formato JSON"
    public List<Item> listar() {
        return itemRepository.findAll();
    }

    // 3. EDITAR
    @PutMapping("/itens/{id}")
    @ResponseBody
    public Item atualizar(@PathVariable Long id, @RequestBody @Valid Item itemAtualizado) {
        itemAtualizado.setId(id);
        return itemRepository.save(itemAtualizado);
    }

    // 4. DELETAR
    @DeleteMapping("/itens/{id}")
    @ResponseBody
    public void deletar(@PathVariable Long id) {
        itemRepository.deleteById(id);
    }

    // 5. BUSCAR UM SÓ
    @GetMapping("/itens/{id}")
    @ResponseBody
    public Item buscarPorId(@PathVariable Long id) {
        return itemRepository.findById(id).orElse(null);
    }
}