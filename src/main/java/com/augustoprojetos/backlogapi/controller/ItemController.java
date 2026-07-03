package com.augustoprojetos.backlogapi.controller;

import com.augustoprojetos.backlogapi.dto.ConquistaDesbloqueadaDTO;
import com.augustoprojetos.backlogapi.service.AtividadeLogService;
import com.augustoprojetos.backlogapi.service.ConquistaService;
import com.augustoprojetos.backlogapi.service.IgdbService;
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
import java.util.stream.Collectors;

@Controller
public class ItemController {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private TmdbService tmdbService;

    @Autowired
    private IgdbService igdbService;

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

        // Verifica se o item pertence ao usuário logado
        if (anterior != null && !anterior.getUser().getId().equals(userLogado.getId())) {
            return ResponseEntity.status(403).body(Map.of("erro", "Acesso negado."));
        }

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
            // Verifica se o item pertence ao usuário logado
            if (userLogado != null && !item.getUser().getId().equals(userLogado.getId())) {
                return;
            }
            if (userLogado != null) {
                atividadeLogService.registrarItemRemovido(userLogado, item);
            }
            itemRepository.deleteById(id);
        }
    }

    // 5. BUSCAR UM
    @GetMapping("/itens/{id}")
    @ResponseBody
    public ResponseEntity<?> buscarPorId(@PathVariable Long id,
                                          @AuthenticationPrincipal User userLogado) {
        Item item = itemRepository.findById(id).orElse(null);
        if (item == null) {
            return ResponseEntity.notFound().build();
        }
        // Verifica se o item pertence ao usuário logado
        if (userLogado == null || !item.getUser().getId().equals(userLogado.getId())) {
            return ResponseEntity.status(403).body(Map.of("erro", "Acesso negado."));
        }
        return ResponseEntity.ok(item);
    }

    // 6. BUSCAR CAPA
    @GetMapping("/api/buscar-capa")
    @ResponseBody
    public List<?> buscarCapa(
        @RequestParam String query,
        @RequestParam(required = false) String tipo) {

        // Se for jogo, chama o serviço IGDB
        if (tipo != null && "Jogo".equalsIgnoreCase(tipo)) {
            return igdbService.buscarJogos(query);
        }

        // Se for Filme ou Série, mantém o fluxo tradicional do TmdbService
        return tmdbService.buscarFilmes(query);
    }

    // 7. LISTAR FILMES SEM DURAÇÃO CADASTRADA (para o preenchimento em lote)
    @GetMapping("/itens/filmes-sem-duracao")
    @ResponseBody
    public List<Item> listarFilmesSemDuracao(@AuthenticationPrincipal User userLogado) {
        return itemRepository.findByUser(userLogado).stream()
                .filter(i -> "Filme".equalsIgnoreCase(i.getTipo()) && i.getDuracaoMinutos() == null)
                .collect(Collectors.toList());
    }

    // 8. BUSCAR SUGESTÕES DE DURAÇÃO NO TMDB
    @PostMapping("/itens/sugestoes-duracao")
    @ResponseBody
    public List<Map<String, Object>> buscarSugestoesDuracao(
            @RequestBody List<Long> ids,
            @AuthenticationPrincipal User userLogado) {

        List<Item> itens = itemRepository.findAllById(ids).stream()
                .filter(i -> i.getUser().getId().equals(userLogado.getId()))
                .filter(i -> "Filme".equalsIgnoreCase(i.getTipo()))
                .collect(Collectors.toList());

        // Busca cada filme em paralelo pra não demorar uma eternidade em acervos grandes
        List<CompletableFuture<Map<String, Object>>> buscas = itens.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> {
                    Integer minutosSugeridos = null;
                    try {
                        minutosSugeridos = tmdbService.buscarDuracaoFilme(item.getTitulo());
                    } catch (Exception ignored) {
                        // Falha silenciosa: item fica sem sugestão, usuário preenche na mão
                    }
                    Map<String, Object> resultado = new HashMap<>();
                    resultado.put("id", item.getId());
                    resultado.put("titulo", item.getTitulo());
                    resultado.put("minutosSugeridos", minutosSugeridos);
                    return resultado;
                }))
                .collect(Collectors.toList());

        return buscas.stream()
                .map(future -> {
                    try {
                        return future.get(8, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        Map<String, Object> semSugestao = new HashMap<>();
                        semSugestao.put("minutosSugeridos", null);
                        return semSugestao;
                    }
                })
                .collect(Collectors.toList());
    }

    // 9. SALVAR EM LOTE APENAS OS FILMES QUE O USUÁRIO SELECIONOU/CONFIRMOU
    public static class AtualizacaoDuracaoDTO {
        private Long id;
        private Integer duracaoMinutos;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Integer getDuracaoMinutos() { return duracaoMinutos; }
        public void setDuracaoMinutos(Integer duracaoMinutos) { this.duracaoMinutos = duracaoMinutos; }
    }

    @PutMapping("/itens/duracao-lote")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> salvarDuracaoLote(
            @RequestBody List<AtualizacaoDuracaoDTO> atualizacoes,
            @AuthenticationPrincipal User userLogado) {

        int atualizados = 0;
        for (AtualizacaoDuracaoDTO dto : atualizacoes) {
            Item item = itemRepository.findById(dto.getId()).orElse(null);
            if (item != null
                    && item.getUser().getId().equals(userLogado.getId())
                    && "Filme".equalsIgnoreCase(item.getTipo())
                    && dto.getDuracaoMinutos() != null
                    && dto.getDuracaoMinutos() >= 0) {
                item.setDuracaoMinutos(dto.getDuracaoMinutos());
                itemRepository.save(item);
                atualizados++;
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("atualizados", atualizados);
        return ResponseEntity.ok(response);
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
