package com.augustoprojetos.backlogapi.service;

import com.augustoprojetos.backlogapi.dto.ConquistaDesbloqueadaDTO;
import com.augustoprojetos.backlogapi.entity.Conquista;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.entity.UserConquista;
import com.augustoprojetos.backlogapi.repository.ConquistaRepository;
import com.augustoprojetos.backlogapi.repository.ItemRepository;
import com.augustoprojetos.backlogapi.repository.UserConquistaRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ConquistaService {

    // --- XP por conquista ---
    private static final int XP_PRIMEIRO_PASSO   = 50;
    private static final int XP_GUERREIRO_FDSEM  = 100;
    private static final int XP_CRITICO_CINEMA   = 75;
    private static final int XP_SEM_TEMPO_IRMAO  = 75;
    private static final int XP_COLECIONADOR     = 100;
    private static final int XP_VICIADO_JOGO     = 100;
    private static final int XP_CINEFILO         = 75;
    private static final int XP_SERIADO          = 75;
    private static final int XP_CRITICO_GERAL    = 150;
    private static final int XP_MARATONISTA      = 200;
    private static final int XP_MENTE_EXPANDIDA  = 50;
    private static final int XP_DESIGNER_VISUAL  = 40;
    private static final int XP_REDE_CONTATOS    = 50;
    private static final int XP_DONO_DO_TEMPO    = 250;
    private static final int XP_PERFECCIONISTA   = 100;

    @Autowired private ConquistaRepository conquistaRepository;
    @Autowired private UserConquistaRepository userConquistaRepository;
    @Autowired private ItemRepository itemRepository;

    // --- Seed das conquistas ---

    @PostConstruct
    public void seedConquistas() {
        upsertConquista("PRIMEIRO_PASSO",   "Primeiro Passo",         "Cadastrou seu primeiro item no backlog.",                     "🎯", XP_PRIMEIRO_PASSO);
        upsertConquista("GUERREIRO_FDSEM",  "Guerreiro de Final de Semana", "Zerou ou assistiu 2 itens consecutivos no mesmo tipo.",  "⚔️", XP_GUERREIRO_FDSEM);
        upsertConquista("CRITICO_CINEMA",   "Crítico de Cinema",      "Deu nota máxima (10) para 5 filmes.",                         "🎬", XP_CRITICO_CINEMA);
        upsertConquista("SEM_TEMPO_IRMAO",  "Sem Tempo, Irmão",       "Marcou 10 itens como Dropado.",                               "💀", XP_SEM_TEMPO_IRMAO);
        upsertConquista("COLECIONADOR",     "Colecionador",            "Cadastrou 50 itens no backlog.",                              "📦", XP_COLECIONADOR);
        upsertConquista("VICIADO_JOGO",     "Viciado em Jogo",        "Zerou 10 jogos.",                                             "🎮", XP_VICIADO_JOGO);
        upsertConquista("CINEFILO",         "Cinéfilo",               "Assistiu 10 filmes.",                                         "🎥", XP_CINEFILO);
        upsertConquista("SERIADO",          "Seriado",                "Assistiu 10 séries.",                                         "📺", XP_SERIADO);
        upsertConquista("CRITICO_GERAL",    "Crítico Geral",          "Deu nota 10 para 10 itens de qualquer tipo.",                 "⭐", XP_CRITICO_GERAL);
        upsertConquista("MARATONISTA",      "Maratonista",            "Concluiu 25 itens no total.",                                 "🏃", XP_MARATONISTA);
        upsertConquista("MENTE_EXPANDIDA", "Mente Expandida",        "Pediu sua primeira recomendação personalizada para a IA.",   "🤖", XP_MENTE_EXPANDIDA);
        upsertConquista("DESIGNER_VISUAL", "Designer Visual",        "Buscou e selecionou uma capa oficial via TMDB para o acervo.", "🖼️", XP_DESIGNER_VISUAL);
        upsertConquista("REDE_CONTATOS",   "Influenciador de Backlog", "Gerou seu primeiro link de compartilhamento para os amigos.",  "🔗", XP_REDE_CONTATOS);
        upsertConquista("DONO_DO_TEMPO",   "Dono do Tempo",           "Concluiu 50 mídias de qualquer tipo no aplicativo.",          "👑", XP_DONO_DO_TEMPO);
        upsertConquista("PERFECCIONISTA",   "Perfeccionista",          "Deu nota 10 para pelo menos 3 jogos (Zerados com maestria).", "🏅", XP_PERFECCIONISTA);
    }

    private void upsertConquista(String chave, String nome, String descricao, String icone, int xp) {
        if (conquistaRepository.findByChave(chave).isEmpty()) {
            Conquista c = new Conquista();
            c.setChave(chave);
            c.setNome(nome);
            c.setDescricao(descricao);
            c.setIcone(icone);
            c.setXp(xp);
            conquistaRepository.save(c);
        }
    }

    @Async
    @Transactional
    public java.util.concurrent.CompletableFuture<List<ConquistaDesbloqueadaDTO>> verificarConquistas(User user) {
        List<ConquistaDesbloqueadaDTO> novas = new ArrayList<>();

        // Coleta dados do usuário em queries otimizadas
        List<Object[]> porTipo   = itemRepository.countItensPorTipo(user.getId());
        List<Object[]> porStatus = itemRepository.countItensPorStatus(user.getId());
        List<Object[]> porNota   = itemRepository.countItensPorNota(user.getId());

        Map<String, Long> countTipo   = toMap(porTipo);
        Map<String, Long> countStatus = toMap(porStatus);
        Map<Double, Long> countNota   = toMapDouble(porNota);

        long totalItens      = countTipo.values().stream().mapToLong(Long::longValue).sum();
        long totalConcluidos = countStatus.getOrDefault("Zerado / Assistido", 0L)
                             + countStatus.getOrDefault("concluido", 0L)
                             + countStatus.getOrDefault("Zerado", 0L)
                             + countStatus.getOrDefault("Assistido", 0L);
        long totalDropados   = countStatus.getOrDefault("Dropado", 0L)
                             + countStatus.getOrDefault("dropado", 0L);
                             
        long jogosZerados    = concluidosDeTipo(user, "Jogo");
        long filmesAssistidos= concluidosDeTipo(user, "Filme");
        long seriesAssistidas= concluidosDeTipo(user, "Série");

        // Conta quantos itens têm nota 10
        long nota10Total = countNota.getOrDefault(10.0, 0L);

        // Conta filmes com nota 10
        long filmeNota10 = itemRepository.countByUserAndTipoAndNota(user, "Filme", 10.0);

        // --- Regras ---

        // 🎯 Primeiro Passo: cadastrou pelo menos 1 item
        if (totalItens >= 1) {
            desbloquear(user, "PRIMEIRO_PASSO", novas);
        }

        // 📦 Colecionador: 50 itens
        if (totalItens >= 50) {
            desbloquear(user, "COLECIONADOR", novas);
        }

        // 🎮 Viciado em Jogo: 10 jogos zerados
        if (jogosZerados >= 10) {
            desbloquear(user, "VICIADO_JOGO", novas);
        }

        // 🎥 Cinéfilo: 10 filmes assistidos
        if (filmesAssistidos >= 10) {
            desbloquear(user, "CINEFILO", novas);
        }

        // 📺 Seriado: 10 séries assistidas
        if (seriesAssistidas >= 10) {
            desbloquear(user, "SERIADO", novas);
        }

        // 🎬 Crítico de Cinema: nota 10 em 5+ filmes
        if (filmeNota10 >= 5) {
            desbloquear(user, "CRITICO_CINEMA", novas);
        }

        // 💀 Sem Tempo, Irmão: 10 dropados
        if (totalDropados >= 10) {
            desbloquear(user, "SEM_TEMPO_IRMAO", novas);
        }

        // ⭐ Crítico Geral: nota 10 em 10 itens
        if (nota10Total >= 10) {
            desbloquear(user, "CRITICO_GERAL", novas);
        }

        // 🏃 Maratonista: 25 concluídos
        if (totalConcluidos >= 25) {
            desbloquear(user, "MARATONISTA", novas);
        }

        // ⚔️ Guerreiro de Final de Semana: 2 itens concluídos do mesmo tipo
        boolean guerreiro = countTipo.entrySet().stream()
            .anyMatch(e -> e.getValue() >= 2 &&
                concluidosDeTipo(user, e.getKey()) >= 2);
        if (guerreiro) {
            desbloquear(user, "GUERREIRO_FDSEM", novas);
        }

        // 👑 Dono do Tempo: 50 itens concluídos no total
        if (totalConcluidos >= 50) {
            desbloquear(user, "DONO_DO_TEMPO", novas);
        }

        // 🏅 Perfeccionista: Nota 10 em pelo menos 3 Jogos
        long jogosNota10 = itemRepository.countByUserAndTipoAndNota(user, "Jogo", 10.0);
        if (jogosNota10 >= 3) {
            desbloquear(user, "PERFECCIONISTA", novas);
        }

        // 🖼️ Designer Visual: Se o usuário tem itens com capa vindos da TMDB
        long itensComCapaTmdb = itemRepository.findByUser(user).stream()
            .filter(item -> item.getImagemUrl() != null && item.getImagemUrl().contains("themoviedb.org"))
            .count();
        if (itensComCapaTmdb >= 1) {
            desbloquear(user, "DESIGNER_VISUAL", novas);
        }

        // Verifica se o usuário já tem o link salvo para dar a conquista de Influenciador
        boolean jaGerouLink = !userConquistaRepository.existsByUserAndConquista_Chave(user, "REDE_CONTATOS");
        // Se a ação veio do ShareTokenService, a chamada vai bater aqui e validar
        if (jaGerouLink) {
             desbloquear(user, "REDE_CONTATOS", novas);
        }

        // Verifica se o usuário já usou a IA para dar a conquista de Mente Expandida
        boolean jaUsouIA = !userConquistaRepository.existsByUserAndConquista_Chave(user, "MENTE_EXPANDIDA");
        // Se a ação veio do RecomendacaoService, a chamada vai bater aqui e validar
        if (jaUsouIA) {
             desbloquear(user, "MENTE_EXPANDIDA", novas);
        }

        return java.util.concurrent.CompletableFuture.completedFuture(novas);
    }

    // --- Helpers ---

    private void desbloquear(User user, String chave, List<ConquistaDesbloqueadaDTO> novas) {
        if (userConquistaRepository.existsByUserAndConquista_Chave(user, chave)) return;

        Conquista c = conquistaRepository.findByChave(chave).orElse(null);
        if (c == null) return;

        userConquistaRepository.save(new UserConquista(user, c));
        novas.add(new ConquistaDesbloqueadaDTO(c.getChave(), c.getNome(), c.getDescricao(), c.getIcone(), c.getXp()));
    }

    private Map<String, Long> toMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(
            r -> (String) r[0],
            r -> (Long) r[1]
        ));
    }

    private Map<Double, Long> toMapDouble(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(
            r -> (Double) r[0],
            r -> (Long) r[1]
        ));
    }

    private long concluidosDeTipo(User user, String tipo) {
        return itemRepository.countByUserAndTipoAndStatusIn(
            user, tipo,
            List.of("Zerado / Assistido", "concluido", "Zerado", "Assistido")
        );
    }

    // --- XP e Nível ---

    public int calcularXpTotal(User user) {
        return userConquistaRepository.sumXpByUserId(user.getId());
    }

    public int calcularNivel(int xpTotal) {
        return (xpTotal / 100) + 1;
    }

    public List<UserConquista> listarConquistasDoUsuario(User user) {
        return userConquistaRepository.findByUser(user);
    }

    // Chamado quando a conta é deletada
    @Transactional
    public void deletarConquistasDoUsuario(User user) {
        userConquistaRepository.deleteByUser(user);
    }
}
