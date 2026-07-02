package com.augustoprojetos.backlogapi.controller;

import com.augustoprojetos.backlogapi.entity.Item;
import com.augustoprojetos.backlogapi.repository.ItemRepository;
import com.augustoprojetos.backlogapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/api/stats")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    // Retorna todos os itens respeitando o filtro de usuário
    private List<Item> getBaseItems(Long usuarioId) {
        if (usuarioId != null) {
            return userRepository.findById(usuarioId)
                    .map(itemRepository::findByUser)
                    .orElse(List.of());
        }
        return itemRepository.findAll();
    }

    // Retorna: { filmes: N, series: N, jogos: N }
    @GetMapping("/tipo")
    public ResponseEntity<Map<String, Object>> statsTipo(
            @RequestParam(name = "tipo", required = false) List<String> tipos,
            @RequestParam(name = "usuarioId", required = false) Long usuarioId) {

        List<String> tiposFiltroRaw = (tipos != null && !tipos.isEmpty())
        ? tipos.stream().map(String::toUpperCase).collect(Collectors.toList())
        : List.of("FILME", "SERIE", "JOGO");

        // Se o front enviar "SERIE", incluímos também "SÉRIE" com acento para bater com o toUpperCase() do banco
        List<String> tiposFiltro = new ArrayList<>();
            for (String t : tiposFiltroRaw) {
            tiposFiltro.add(t);
            if ("SERIE".equals(t)) {
                tiposFiltro.add("SÉRIE");
            }
        }

        List<Item> items = getBaseItems(usuarioId).stream()
            .filter(i -> i.getTipo() != null && tiposFiltro.contains(i.getTipo().toUpperCase()))
            .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("filmes", items.stream().filter(i -> "FILME".equalsIgnoreCase(i.getTipo())).count());
        result.put("series", items.stream().filter(i -> "SÉRIE".equalsIgnoreCase(i.getTipo()) || "SERIE".equalsIgnoreCase(i.getTipo())).count());
        result.put("jogos",  items.stream().filter(i -> "JOGO".equalsIgnoreCase(i.getTipo())).count());
        return ResponseEntity.ok(result);
    }

    // Retorna: { assistidos: N, assistindo: N, backlog: N, dropados: N }
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> statsStatus(
            @RequestParam(name = "status", required = false) List<String> statusList,
            @RequestParam(name = "tipo",   required = false) List<String> tipos,
            @RequestParam(name = "usuarioId", required = false) Long usuarioId) {

        List<String> tiposFiltro = (tipos != null && !tipos.isEmpty())
                ? tipos.stream().map(String::toUpperCase).collect(Collectors.toList())
                : List.of("FILME","SERIE","SÉRIE","JOGO");

        List<String> statusFiltro = (statusList != null && !statusList.isEmpty())
                ? statusList.stream().map(String::toUpperCase).collect(Collectors.toList())
                : List.of("CONCLUIDO", "ASSISTIDO", "ZERADO", "ASSISTINDO", "JOGANDO", "BACKLOG", "DROPADO");

        List<Item> items = getBaseItems(usuarioId).stream()
                .filter(i -> {
                    if (i.getStatus() == null) return false;
                    String sUpper = i.getStatus().toUpperCase();
                    if (statusFiltro.contains("CONCLUIDO") && (sUpper.contains("ASSISTIDO") || sUpper.contains("ZERADO"))) return true;
                    if (statusFiltro.contains("ASSISTINDO") && (sUpper.contains("ASSISTINDO") || sUpper.contains("JOGANDO"))) return true;
                    return statusFiltro.contains(sUpper);
                })
                .filter(i -> i.getTipo() != null && tiposFiltro.contains(i.getTipo().toUpperCase()))
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("assistidos", items.stream().filter(i -> i.getStatus().toUpperCase().contains("ASSISTIDO") || i.getStatus().toUpperCase().contains("ZERADO")).count());
        result.put("assistindo", items.stream().filter(i -> i.getStatus().toUpperCase().contains("ASSISTINDO") || i.getStatus().toUpperCase().contains("JOGANDO")).count());
        result.put("backlog",    items.stream().filter(i -> i.getStatus().toUpperCase().contains("BACKLOG")).count());
        result.put("dropados",   items.stream().filter(i -> i.getStatus().toUpperCase().contains("DROPADO")).count());
        return ResponseEntity.ok(result);
    }

    // Retorna: { "10.0": 5, "9.5": 3, ... }
    @GetMapping("/notas")
    public ResponseEntity<Map<String, Object>> statsNotas(
            @RequestParam(name = "tipo",   required = false) List<String> tipos,
            @RequestParam(name = "status", required = false) List<String> statusList,
            @RequestParam(name = "usuarioId", required = false) Long usuarioId,
            @RequestParam(name = "de",  defaultValue = "0")  Double de,
            @RequestParam(name = "ate", defaultValue = "10") Double ate) {

        List<String> tiposFiltro = (tipos != null && !tipos.isEmpty())
                ? tipos.stream().map(String::toUpperCase).collect(Collectors.toList())
                : List.of("FILME","SERIE","SÉRIE","JOGO");

        List<String> statusFiltro = (statusList != null && !statusList.isEmpty())
                ? statusList.stream().map(String::toUpperCase).collect(Collectors.toList())
                : List.of("CONCLUIDO", "ASSISTIDO", "ZERADO", "ASSISTINDO", "JOGANDO", "BACKLOG", "DROPADO");

        final double deVal  = de  != null ? de  : 0.0;
        final double ateVal = ate != null ? ate : 10.0;

        List<Item> items = getBaseItems(usuarioId).stream()
                .filter(i -> i.getTipo() != null && tiposFiltro.contains(i.getTipo().toUpperCase()))
                .filter(i -> {
                    if (i.getStatus() == null) return false;
                    String sUpper = i.getStatus().toUpperCase();
                    if (statusFiltro.contains("CONCLUIDO") && (sUpper.contains("ASSISTIDO") || sUpper.contains("ZERADO"))) return true;
                    if (statusFiltro.contains("ASSISTINDO") && (sUpper.contains("ASSISTINDO") || sUpper.contains("JOGANDO"))) return true;
                    return statusFiltro.contains(sUpper);
                })
                .filter(i -> i.getNota() != null && i.getNota() > 0)
                .filter(i -> i.getNota() >= deVal && i.getNota() <= ateVal)
                .toList();

        Map<String, Object> result = items.stream()
                .collect(Collectors.groupingBy(
                        i -> {
                            double n = i.getNota();
                            return n == Math.floor(n) ? String.valueOf((int) n) : String.valueOf(n);
                        },
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted((a, b) -> Double.compare(
                        Double.parseDouble(b.getKey()),
                        Double.parseDouble(a.getKey())))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> (Object) e.getValue(),
                        (x, y) -> x,
                        LinkedHashMap::new));

        return ResponseEntity.ok(result);
    }
}
