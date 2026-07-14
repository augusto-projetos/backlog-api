package com.augustoprojetos.backlogapi.controller;

import com.augustoprojetos.backlogapi.dto.DashboardStatsDTO;
import com.augustoprojetos.backlogapi.entity.Item;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.repository.ItemRepository;
import com.augustoprojetos.backlogapi.repository.UserRepository;
import com.augustoprojetos.backlogapi.util.NotaScaleUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<DashboardStatsDTO> getEstatisticas() {
        // 1. Pega o usuário logado
        User user = usuarioLogado();

        // 2. Cria o DTO vazio
        DashboardStatsDTO stats = new DashboardStatsDTO();

        if (user == null) return ResponseEntity.notFound().build();

        // 3. Processa TIPOS
        List<Object[]> listaTipos = itemRepository.countItensPorTipo(user.getId());
        for (Object[] obj : listaTipos) {
            String tipo = (String) obj[0];
            Long qtd = (Long) obj[1];

            if ("Jogo".equalsIgnoreCase(tipo)) stats.setTotalJogos(qtd);
            else if ("Filme".equalsIgnoreCase(tipo)) stats.setTotalFilmes(qtd);
            else if ("Série".equalsIgnoreCase(tipo)) stats.setTotalSeries(qtd);
        }

        // 4. Processa STATUS
        List<Object[]> listaStatus = itemRepository.countItensPorStatus(user.getId());
        for (Object[] obj : listaStatus) {
            String status = (String) obj[0];
            Long qtd = (Long) obj[1];

            if (status.contains("Zerado") || status.contains("Assistido")) {
                stats.setTotalZerados(stats.getTotalZerados() + qtd);
            } else if (status.contains("Jogando") || status.contains("Assistindo")) {
                stats.setTotalJogando(stats.getTotalJogando() + qtd);
            } else if (status.contains("Backlog")) {
                stats.setTotalBacklog(qtd);
            } else if (status.contains("Dropado")) {
                stats.setTotalDropados(qtd);
            }
        }

        // 5. Processa as NOTAS (somente notas com itens, da maior para a menor)
        List<Object[]> listaNotas = itemRepository.countItensPorNota(user.getId());

        Map<Double, Long> contagemPorNota = new LinkedHashMap<>();
        for (Object[] obj : listaNotas) {
            Double nota = (Double) obj[0];
            Long qtd = (Long) obj[1];
            contagemPorNota.put(nota, qtd);
        }

        stats.setNotas(NotaScaleUtil.apenasComItens(contagemPorNota));

        // 6. Processa o TEMPO GASTO (Filmes, Jogos e Séries informados manualmente pelo
        // usuário, sempre em minutos).
        List<com.augustoprojetos.backlogapi.entity.Item> itensUsuario = itemRepository.findByUser(user);

        // Itens em Backlog ou Dropados não contam tempo investido
        long minutosFilmes = itensUsuario.stream()
                .filter(i -> "Filme".equalsIgnoreCase(i.getTipo()) && i.getDuracaoMinutos() != null)
                .filter(this::contaNoTempoInvestido)
                .mapToLong(com.augustoprojetos.backlogapi.entity.Item::getDuracaoMinutos)
                .sum();

        long minutosJogos = itensUsuario.stream()
                .filter(i -> "Jogo".equalsIgnoreCase(i.getTipo()) && i.getMinutosJogados() != null)
                .filter(this::contaNoTempoInvestido)
                .mapToLong(com.augustoprojetos.backlogapi.entity.Item::getMinutosJogados)
                .sum();

        // Séries
        long minutosSeries = itensUsuario.stream()
                .filter(i -> "Série".equalsIgnoreCase(i.getTipo()) && i.getDuracaoTotalMinutos() != null)
                .filter(this::contaNoTempoInvestido)
                .mapToLong(com.augustoprojetos.backlogapi.entity.Item::getDuracaoTotalMinutos)
                .sum();

        stats.setMinutosFilmes(minutosFilmes);
        stats.setMinutosJogos(minutosJogos);
        stats.setMinutosSeries(minutosSeries);
        stats.setTempoSeriesDisponivel(true);

        return ResponseEntity.ok(stats);
    }

    // --- ENDPOINTS FILTRÁVEIS POR TIPO ---

    // Retorna: { zerados: N, jogando: N, backlog: N, dropados: N }
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatusFiltrado(
            @RequestParam(name = "tipo", required = false) List<String> tipos) {

        User user = usuarioLogado();
        if (user == null) return ResponseEntity.notFound().build();

        List<Item> itens = filtrarPorTipo(itemRepository.findByUser(user), tipos);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("zerados", itens.stream().filter(this::isConcluido).count());
        result.put("jogando", itens.stream().filter(this::isEmAndamento).count());
        result.put("backlog", itens.stream().filter(i -> i.getStatus() != null && i.getStatus().contains("Backlog")).count());
        result.put("dropados", itens.stream().filter(i -> i.getStatus() != null && i.getStatus().contains("Dropado")).count());
        return ResponseEntity.ok(result);
    }

    // Retorna: { "10": 5, "9.5": 3, ..., "0.5": 0 } - sempre com a escala completa
    @GetMapping("/notas")
    public ResponseEntity<Map<String, Object>> getNotasFiltrado(
            @RequestParam(name = "tipo", required = false) List<String> tipos) {

        User user = usuarioLogado();
        if (user == null) return ResponseEntity.notFound().build();

        List<Item> itens = filtrarPorTipo(itemRepository.findByUser(user), tipos);

        Map<Double, Long> contagemPorNota = itens.stream()
                .filter(i -> i.getNota() != null && i.getNota() > 0)
                .collect(Collectors.groupingBy(Item::getNota, Collectors.counting()));

        Map<String, Object> result = new LinkedHashMap<>();
        NotaScaleUtil.apenasComItens(contagemPorNota).forEach(result::put);
        return ResponseEntity.ok(result);
    }

    // --- Helpers ---

    private User usuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    // Filtra os itens pelo(s) tipo(s) informado(s).
    private List<Item> filtrarPorTipo(List<Item> itens, List<String> tipos) {
        if (tipos == null || tipos.isEmpty()) return itens;
        return itens.stream()
                .filter(i -> i.getTipo() != null && tipos.stream().anyMatch(t -> t.equalsIgnoreCase(i.getTipo())))
                .collect(Collectors.toList());
    }

    private boolean isConcluido(Item item) {
        String status = item.getStatus();
        return status != null && (status.contains("Zerado") || status.contains("Assistido"));
    }

    private boolean isEmAndamento(Item item) {
        String status = item.getStatus();
        return status != null && (status.contains("Jogando") || status.contains("Assistindo"));
    }

    // Método auxiliar para verificar se o item deve contar no tempo investido
    private boolean contaNoTempoInvestido(com.augustoprojetos.backlogapi.entity.Item item) {
        String status = item.getStatus();
        if (status == null) return true;
        String statusLower = status.toLowerCase();
        return !statusLower.contains("backlog") && !statusLower.contains("dropado") &&
                !statusLower.contains("assistindo") && !statusLower.contains("jogando");
    }
}
