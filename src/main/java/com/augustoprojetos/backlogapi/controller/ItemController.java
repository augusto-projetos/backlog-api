package com.augustoprojetos.backlogapi.controller;

import com.augustoprojetos.backlogapi.dto.ConquistaDesbloqueadaDTO;
import com.augustoprojetos.backlogapi.service.ConquistaService;
import com.augustoprojetos.backlogapi.service.TmdbService;
import org.springframework.ui.Model;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.entity.Item;
import com.augustoprojetos.backlogapi.repository.ItemRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Controller
public class ItemController {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private TmdbService tmdbService;

    @Autowired
    private ConquistaService conquistaService;

    // --- ROTAS DE PÁGINAS (VIEW) ---

    @GetMapping("/")
    public String index() { return "index"; }

    @GetMapping("/home")
    public String home(Model model, @AuthenticationPrincipal User userLogado) {
        if (userLogado != null) {
            model.addAttribute("nomeUsuario", userLogado.getLogin());
        } else {
            model.addAttribute("nomeUsuario", "Visitante");
        }

        return "home";
    }

    @GetMapping("/cadastro")
    public String paginaCadastro() { return "cadastro"; }

    @GetMapping("/creditos")
    public String exibirCreditos(Model model, @AuthenticationPrincipal User userLogado) {
        if (userLogado != null) {
            model.addAttribute("nomeUsuario", userLogado.getLogin());
        } else {
            model.addAttribute("nomeUsuario", "Visitante");
        }

        return "creditos";
    }

    // --- ROTAS DA API (JSON) COM SEGURANÇA ---

    // 1. CADASTRAR - salva o item e verifica conquistas em background.
    @PostMapping("/itens")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cadastrar(
            @RequestBody @Valid Item item,
            @AuthenticationPrincipal User userLogado) {

        item.setUser(userLogado);
        Item salvo = itemRepository.save(item);

        List<ConquistaDesbloqueadaDTO> novas = verificarConquistasComTimeout(userLogado);

        Map<String, Object> response = new HashMap<>();
        response.put("item", salvo);
        response.put("conquistasDesbloqueadas", novas);
        return ResponseEntity.ok(response);
    }

    // 2. LISTAR
    @GetMapping("/itens")
    @ResponseBody
    public List<Item> listar(@AuthenticationPrincipal User userLogado) {
        return itemRepository.findByUser(userLogado);
    }

    // 3. EDITAR — atualiza o item e verifica conquistas (nota 10 pode desbloquear algo).
    @PutMapping("/itens/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid Item itemAtualizado,
            @AuthenticationPrincipal User userLogado) {

        itemAtualizado.setId(id);
        itemAtualizado.setUser(userLogado);
        Item salvo = itemRepository.save(itemAtualizado);

        List<ConquistaDesbloqueadaDTO> novas = verificarConquistasComTimeout(userLogado);

        Map<String, Object> response = new HashMap<>();
        response.put("item", salvo);
        response.put("conquistasDesbloqueadas", novas);
        return ResponseEntity.ok(response);
    }

    // 4. DELETAR
    @DeleteMapping("/itens/{id}")
    @ResponseBody
    public void deletar(@PathVariable Long id) {
        itemRepository.deleteById(id);
    }

    // 5. BUSCAR UM
    @GetMapping("/itens/{id}")
    @ResponseBody
    public Item buscarPorId(@PathVariable Long id) {
        return itemRepository.findById(id).orElse(null);
    }

    // 6. BUSCAR CAPA
    @GetMapping("/api/buscar-capa")
    @ResponseBody
    public List<TmdbService.SearchResult> buscarCapa(
            @RequestParam String query,
            @RequestParam(required = false) String tipo) {
        if (tipo != null && "Jogo".equalsIgnoreCase(tipo)) {
            return java.util.Collections.emptyList();
        }
        return tmdbService.buscarFilmes(query);
    }

    // Aguarda até 2 segundos pela verificação assíncrona de conquistas.
    // Se expirar, retorna lista vazia (o usuário não perde nada, só não vê o toast agora).
    
    private List<ConquistaDesbloqueadaDTO> verificarConquistasComTimeout(User user) {
        try {
            CompletableFuture<List<ConquistaDesbloqueadaDTO>> future =
                conquistaService.verificarConquistas(user);
            return future.get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            return List.of();
        }
    }
}