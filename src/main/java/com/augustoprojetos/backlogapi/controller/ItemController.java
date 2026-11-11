package com.augustoprojetos.backlogapi.controller;

import com.augustoprojetos.backlogapi.dto.AtualizacaoDuracaoDTO;
import com.augustoprojetos.backlogapi.dto.ItemRequest;
import com.augustoprojetos.backlogapi.entity.Item;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.service.IgdbService;
import com.augustoprojetos.backlogapi.service.ItemService;
import com.augustoprojetos.backlogapi.service.ItemService.ResultadoItem;
import com.augustoprojetos.backlogapi.service.ItemService.ResultadoLote;
import com.augustoprojetos.backlogapi.service.TmdbService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class ItemController {

    private final ItemService itemService;
    private final TmdbService tmdbService;
    private final IgdbService igdbService;

    public ItemController(ItemService itemService, TmdbService tmdbService, IgdbService igdbService) {
        this.itemService = itemService;
        this.tmdbService = tmdbService;
        this.igdbService = igdbService;
    }

    // --- ROTAS DE PÁGINAS (VIEW) ---

    @GetMapping("/")
    public String index() { return "index"; }

    @GetMapping("/home")
    public String home(Model model, @AuthenticationPrincipal User userLogado) {
        adicionarNomeUsuario(model, userLogado);
        return "home";
    }

    @GetMapping("/cadastro")
    public String paginaCadastro() { return "cadastro"; }

    @GetMapping("/creditos")
    public String exibirCreditos(Model model, @AuthenticationPrincipal User userLogado) {
        adicionarNomeUsuario(model, userLogado);
        return "creditos";
    }

    private void adicionarNomeUsuario(Model model, User userLogado) {
        model.addAttribute("nomeUsuario", userLogado != null ? userLogado.getLogin() : "Visitante");
    }

    // --- ROTAS DA API (JSON) COM SEGURANÇA ---

    // 1. CADASTRAR
    @PostMapping("/itens")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cadastrar(
            @RequestBody @Valid ItemRequest requisicao,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal User userLogado) {

        ResultadoItem resultado = itemService.cadastrar(requisicao, idempotencyKey, userLogado);
        return ResponseEntity.ok(respostaItem(resultado));
    }

    // 2. LISTAR
    @GetMapping("/itens")
    @ResponseBody
    public List<Item> listar(@AuthenticationPrincipal User userLogado) {
        return itemService.listar(userLogado);
    }

    // 3. EDITAR
    @PutMapping("/itens/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid ItemRequest requisicao,
            @AuthenticationPrincipal User userLogado) {

        ResultadoItem resultado = itemService.atualizar(id, requisicao, userLogado);

        if (resultado.status() == ItemService.Status.NAO_ENCONTRADO) {
            return ResponseEntity.notFound().build();
        }
        if (!resultado.sucesso()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("erro", "Acesso negado."));
        }
        return ResponseEntity.ok(respostaItem(resultado));
    }

    // 4. DELETAR
    @DeleteMapping("/itens/{id}")
    @ResponseBody
    public void deletar(@PathVariable Long id, @AuthenticationPrincipal User userLogado) {
        itemService.deletar(id, userLogado);
    }

    // 5. BUSCAR UM
    @GetMapping("/itens/{id}")
    @ResponseBody
    public ResponseEntity<?> buscarPorId(@PathVariable Long id, @AuthenticationPrincipal User userLogado) {
        Optional<Item> item = itemService.buscarPorId(id);
        if (item.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!itemService.pertenceA(item.get(), userLogado)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("erro", "Acesso negado."));
        }
        return ResponseEntity.ok(item.get());
    }

    // 6. BUSCAR CAPA
    @GetMapping("/api/buscar-capa")
    @ResponseBody
    public List<?> buscarCapa(@RequestParam String query, @RequestParam(required = false) String tipo) {
        // Jogo → IGDB; Filme ou Série → TMDB
        if ("Jogo".equalsIgnoreCase(tipo)) {
            return igdbService.buscarJogos(query);
        }

        // Se for Filme ou Série, mantém o fluxo tradicional do TmdbService
        return tmdbService.buscarFilmes(query);
    }

    // 7. LISTAR FILMES SEM DURAÇÃO CADASTRADA (para o preenchimento em lote)
    @GetMapping("/itens/filmes-sem-duracao")
    @ResponseBody
    public List<Item> listarFilmesSemDuracao(@AuthenticationPrincipal User userLogado) {
        return itemService.listarFilmesSemDuracao(userLogado);
    }

    // 8. BUSCAR SUGESTÕES DE DURAÇÃO NO TMDB
    @PostMapping("/itens/sugestoes-duracao")
    @ResponseBody
    public List<Map<String, Object>> buscarSugestoesDuracao(
            @RequestBody List<Long> ids,
            @AuthenticationPrincipal User userLogado) {
        return itemService.sugerirDuracoes(ids, userLogado);
    }

    // 9. SALVAR EM LOTE APENAS OS FILMES QUE O USUÁRIO SELECIONOU/CONFIRMOU
    @PutMapping("/itens/duracao-lote")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> salvarDuracaoLote(
            @RequestBody List<AtualizacaoDuracaoDTO> atualizacoes,
            @AuthenticationPrincipal User userLogado) {

        ResultadoLote resultado = itemService.salvarDuracaoLote(atualizacoes, userLogado);

        Map<String, Object> response = new HashMap<>();
        response.put("atualizados", resultado.atualizados());
        response.put("conquistasDesbloqueadas", resultado.conquistas());
        return ResponseEntity.ok(response);
    }

    // 10. SALVAR A DURAÇÃO DE UM ÚNICO FILME
    @PutMapping("/itens/{id}/duracao")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> salvarDuracaoUnica(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User userLogado) {

        Object minutosBruto = body.get("duracaoMinutos");
        Integer minutos = (minutosBruto instanceof Number numero && numero.intValue() >= 0)
                ? Integer.valueOf(numero.intValue())
                : null;
        Object grupoIdBruto = body.get("grupoId");
        String grupoId = grupoIdBruto instanceof String s && !s.isBlank() ? s : null;

        ResultadoItem resultado = itemService.salvarDuracaoUnica(id, minutos, grupoId, userLogado);

        Map<String, Object> response = new HashMap<>();
        response.put("sucesso", resultado.sucesso());
        if (!resultado.sucesso()) {
            return ResponseEntity.status(statusHttpDeFalha(resultado)).body(response);
        }
        response.put("conquistasDesbloqueadas", resultado.conquistas());
        return ResponseEntity.ok(response);
    }

    // 11. AVANÇAR +1 EPISÓDIO DE UMA SÉRIE (atualização rápida via AJAX na Home)
    @PostMapping("/itens/{id}/proximo-episodio")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> avancarEpisodio(
            @PathVariable Long id,
            @AuthenticationPrincipal User userLogado) {

        ResultadoItem resultado = itemService.avancarEpisodio(id, userLogado);

        Map<String, Object> response = new HashMap<>();
        response.put("sucesso", resultado.sucesso());
        if (!resultado.sucesso()) {
            if (resultado.erro() != null) {
                response.put("erro", resultado.erro());
            }
            return ResponseEntity.status(statusHttpDeFalha(resultado)).body(response);
        }

        Item salvo = resultado.item();
        response.put("temporadaAtual", salvo.getTemporadaAtual());
        response.put("episodioAtual", salvo.getEpisodioAtual());
        response.put("conquistasDesbloqueadas", resultado.conquistas());
        return ResponseEntity.ok(response);
    }

    // --- Helpers ---

    // Mesmo formato de resposta de cadastrar/atualizar
    private Map<String, Object> respostaItem(ResultadoItem resultado) {
        Map<String, Object> response = new HashMap<>();
        response.put("item", resultado.item());
        response.put("conquistasDesbloqueadas", resultado.conquistas());
        return response;
    }

    // Item inexistente ou de outro usuário → 403 (como sempre); regra de negócio violada → 400
    private HttpStatus statusHttpDeFalha(ResultadoItem resultado) {
        return resultado.status() == ItemService.Status.REQUISICAO_INVALIDA
                ? HttpStatus.BAD_REQUEST
                : HttpStatus.FORBIDDEN;
    }
}
