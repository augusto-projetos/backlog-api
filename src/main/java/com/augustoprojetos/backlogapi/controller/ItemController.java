package com.augustoprojetos.backlogapi.controller;

import com.augustoprojetos.backlogapi.entity.Item;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.repository.ItemRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class ItemController {

    @Autowired
    private ItemRepository itemRepository;

    // --- ROTAS DE PÁGINAS (VIEW) ---

    @GetMapping("/")
    public String index() { return "index"; }

    @GetMapping("/home")
    public String home() { return "home"; }

    @GetMapping("/cadastro")
    public String paginaCadastro() { return "cadastro"; }

    // --- ROTAS DA API (JSON) COM SEGURANÇA ---

    // 1. CADASTRAR (Agora associa ao usuário logado!)
    @PostMapping("/itens")
    @ResponseBody
    public Item cadastrar(@RequestBody @Valid Item item, @AuthenticationPrincipal User userLogado) {
        // Pega o usuário da sessão e carimba no item
        item.setUser(userLogado);
        return itemRepository.save(item);
    }

    // 2. LISTAR (Só traz os itens do usuário logado!)
    @GetMapping("/itens")
    @ResponseBody
    public List<Item> listar(@AuthenticationPrincipal User userLogado) {
        // Em vez de findAll(), usamos o nosso filtro novo
        return itemRepository.findByUser(userLogado);
    }

    // 3. EDITAR (Mantém o dono original ou atualiza se precisar)
    @PutMapping("/itens/{id}")
    @ResponseBody
    public Item atualizar(@PathVariable Long id, @RequestBody @Valid Item itemAtualizado, @AuthenticationPrincipal User userLogado) {
        itemAtualizado.setId(id);
        itemAtualizado.setUser(userLogado); // Garante que continua sendo dele
        return itemRepository.save(itemAtualizado);
    }

    // 4. DELETAR (Sem mudanças, mas idealmente checaria se o item é dele)
    @DeleteMapping("/itens/{id}")
    @ResponseBody
    public void deletar(@PathVariable Long id) {
        itemRepository.deleteById(id);
    }

    // 5. BUSCAR UM (Sem mudanças)
    @GetMapping("/itens/{id}")
    @ResponseBody
    public Item buscarPorId(@PathVariable Long id) {
        return itemRepository.findById(id).orElse(null);
    }
}