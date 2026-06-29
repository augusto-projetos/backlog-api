package com.augustoprojetos.backlogapi.controller;

import com.augustoprojetos.backlogapi.dto.ConquistaDesbloqueadaDTO;
import com.augustoprojetos.backlogapi.service.AtividadeLogService;
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

    @Autowired
    private AtividadeLogService atividadeLogService;

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

    // 1. CADASTRAR
    @PostMapping("/itens")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cadastrar(
            @RequestBody @Valid Item item,
            @AuthenticationPrincipal User userLogado) {

        item.setUser(userLogado);
        Item salvo = itemRepository.save(item);

        // Registra na timeline
        atividadeLogService.registrarItemAdicionado(userLogado, salvo);

        List<ConquistaDesbloqueadaDTO> novas = verificarConquistasComTimeout(userLogado);

        // Registra conquistas desbloqueadas na timeline
        novas.forEach(c -> atividadeLogService.registrarConquistaDesbloqueada(
                userLogado, c.nome(), c.icone()));

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

    // 3. EDITAR
    @PutMapping("/itens/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid Item itemAtualizado,
            @AuthenticationPrincipal User userLogado) {

        // Carrega o item anterior para comparar campos
        Item anterior = itemRepository.findById(id).orElse(null);

        itemAtualizado.setId(id);
        itemAtualizado.setUser(userLogado);
        Item salvo = itemRepository.save(itemAtualizado);

        // Detecta o que mudou e registra o evento mais específico
        if (anterior != null) {
            boolean statusMudou = !igual(anterior.getStatus(), salvo.getStatus());
            boolean notaMudou   = !igual(anterior.getNota(), salvo.getNota());
            boolean resenhaMudou = !igual(anterior.getResenha(), salvo.getResenha());

            if (statusMudou) {
                atividadeLogService.registrarStatusAlterado(userLogado, salvo, anterior.getStatus());
            }
            if (notaMudou) {
                atividadeLogService.registrarNotaAlterada(userLogado, salvo, anterior.getNota());
            }
            if (resenhaMudou) {
                boolean resenhaEraVazia = anterior.getResenha() == null || anterior.getResenha().isBlank();
                if (resenhaEraVazia && salvo.getResenha() != null && !salvo.getResenha().isBlank()) {
                    atividadeLogService.registrarResenhaAdicionada(userLogado, salvo);
                } else if (salvo.getResenha() != null && !salvo.getResenha().isBlank()) {
                    atividadeLogService.registrarResenhaEditada(userLogado, salvo);
                }
            }
            // Se mudou outros campos mas nenhum dos acima
            if (!statusMudou && !notaMudou && !resenhaMudou) {
                atividadeLogService.registrarItemEditado(userLogado, salvo);
            }
        } else {
            atividadeLogService.registrarItemEditado(userLogado, salvo);
        }

        List<ConquistaDesbloqueadaDTO> novas = verificarConquistasComTimeout(userLogado);

        novas.forEach(c -> atividadeLogService.registrarConquistaDesbloqueada(
                userLogado, c.nome(), c.icone()));

        Map<String, Object> response = new HashMap<>();
        response.put("item", salvo);
        response.put("conquistasDesbloqueadas", novas);
        return ResponseEntity.ok(response);
    }

    // 4. DELETAR
    @DeleteMapping("/itens/{id}")
    @ResponseBody
    public void deletar(@PathVariable Long id,
                        @AuthenticationPrincipal User userLogado) {
        Item item = itemRepository.findById(id).orElse(null);
        if (item != null) {
            if (userLogado != null) {
                atividadeLogService.registrarItemRemovido(userLogado, item);
            }
            itemRepository.deleteById(id);
        } else {
            itemRepository.deleteById(id);
        }
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

    // --- Helpers ---

    private List<ConquistaDesbloqueadaDTO> verificarConquistasComTimeout(User user) {
        try {
            CompletableFuture<List<ConquistaDesbloqueadaDTO>> future =
                conquistaService.verificarConquistas(user);
            return future.get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            return List.of();
        }
    }

    // Compara dois objetos com null-safety
    private boolean igual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}