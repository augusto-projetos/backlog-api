package com.augustoprojetos.backlogapi.service;

import com.augustoprojetos.backlogapi.dto.ConquistaDesbloqueadaDTO;
import com.augustoprojetos.backlogapi.entity.Conquista;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.entity.UserConquista;
import com.augustoprojetos.backlogapi.repository.ConquistaRepository;
import com.augustoprojetos.backlogapi.repository.ItemRepository;
import com.augustoprojetos.backlogapi.repository.ShareTokenRepository;
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

    @Autowired private ConquistaRepository conquistaRepository;
    @Autowired private UserConquistaRepository userConquistaRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private ShareTokenRepository shareTokenRepository;

    // ---------------------------------------------------------------
    // Seed das conquistas fixas
    // criterioTipo define COMO o sistema verifica o desbloqueio.
    //
    // Tipos de critério disponíveis (usado no switch de verificarConquistas):
    //   TOTAL_ITENS          - total de itens cadastrados >= criterioValor
    //   TOTAL_CONCLUIDOS     - total de itens concluídos >= criterioValor
    //   TOTAL_DROPADOS       - total de itens dropados >= criterioValor
    //   JOGOS_ZERADOS        - jogos zerados >= criterioValor
    //   FILMES_ASSISTIDOS    - filmes assistidos >= criterioValor
    //   SERIES_ASSISTIDAS    - séries assistidas >= criterioValor
    //   NOTA10_FILMES        - filmes com nota 10 >= criterioValor
    //   NOTA10_JOGOS         - jogos com nota 10 >= criterioValor
    //   NOTA10_TOTAL         - qualquer item com nota 10 >= criterioValor
    //   TMDB_CAPA            - usuário tem >= 1 item com imagemUrl do themoviedb.org
    //   AI_USADA             - detectado no momento da chamada à IA (disparo direto)
    //   SHARE_LINK_CRIADO    - usuário tem >= 1 share_token no banco
    //   MANUAL               - concedida manualmente pelo admin
    // ---------------------------------------------------------------

    @PostConstruct
    public void seedConquistas() {
        upsertConquista("PRIMEIRO_PASSO",  "Primeiro Passo",               "Cadastrou seu primeiro item no backlog.",                      "🎯", 50,  "TOTAL_ITENS",       1);
        upsertConquista("GUERREIRO_FDSEM", "Guerreiro de Final de Semana", "Zerou ou assistiu 2 itens no total.",                         "⚔️", 100, "TOTAL_CONCLUIDOS",  2);
        upsertConquista("CRITICO_CINEMA",  "Crítico de Cinema",            "Deu nota máxima (10) para 5 filmes.",                         "🎬", 75,  "NOTA10_FILMES",     5);
        upsertConquista("SEM_TEMPO_IRMAO", "Sem Tempo, Irmão",             "Marcou 10 itens como Dropado.",                               "💀", 75,  "TOTAL_DROPADOS",    10);
        upsertConquista("COLECIONADOR",    "Colecionador",                 "Cadastrou 50 itens no backlog.",                              "📦", 100, "TOTAL_ITENS",       50);
        upsertConquista("VICIADO_JOGO",    "Viciado em Jogo",              "Zerou 10 jogos.",                                             "🎮", 100, "JOGOS_ZERADOS",     10);
        upsertConquista("CINEFILO",        "Cinéfilo",                     "Assistiu 10 filmes.",                                         "🎥", 75,  "FILMES_ASSISTIDOS", 10);
        upsertConquista("SERIADO",         "Seriado",                      "Assistiu 10 séries.",                                         "📺", 75,  "SERIES_ASSISTIDAS", 10);
        upsertConquista("CRITICO_GERAL",   "Crítico Geral",                "Deu nota 10 para 10 itens de qualquer tipo.",                 "⭐", 150, "NOTA10_TOTAL",      10);
        upsertConquista("MARATONISTA",     "Maratonista",                  "Concluiu 25 itens no total.",                                 "🏃", 200, "TOTAL_CONCLUIDOS",  25);
        upsertConquista("MENTE_EXPANDIDA", "Mente Expandida",              "Pediu sua primeira recomendação personalizada para a IA.",    "🤖", 50,  "AI_USADA",          1);
        upsertConquista("DESIGNER_VISUAL", "Designer Visual",              "Buscou e selecionou uma capa oficial via TMDB para o acervo.","🖼️", 40, "TMDB_CAPA",         1);
        upsertConquista("REDE_CONTATOS",   "Influenciador de Backlog",     "Gerou seu primeiro link de compartilhamento para os amigos.", "🔗", 50,  "SHARE_LINK_CRIADO", 1);
        upsertConquista("DONO_DO_TEMPO",   "Dono do Tempo",                "Concluiu 50 mídias de qualquer tipo no aplicativo.",          "👑", 250, "TOTAL_CONCLUIDOS",  50);
        upsertConquista("PERFECCIONISTA",  "Perfeccionista",               "Deu nota 10 para pelo menos 3 jogos (Zerados com maestria).", "🏅", 100, "NOTA10_JOGOS",      3);
        upsertConquista("LENDA_VIVA",      "Lenda Viva",                   "Concluiu 100 mídias. Você é uma máquina!",                    "🦾", 500, "TOTAL_CONCLUIDOS",  100);
        upsertConquista("ARQUIVO_MORTO",   "Arquivo Morto",                "Cadastrou 200 itens. Isso é um acervo sério.",                "🗄️", 300, "TOTAL_ITENS",       200);
        upsertConquista("CINEFILO_ELITE",  "Cinéfilo de Elite",            "Assistiu 50 filmes. Hollywood te deve uma cadeira VIP.",      "🎞️", 300, "FILMES_ASSISTIDOS", 50);
        upsertConquista("GAMER_HARDCORE",  "Gamer Hardcore",               "Zerou 30 jogos. Você não conhece a palavra parar.",           "🕹️", 300, "JOGOS_ZERADOS",     30);
        upsertConquista("BINGE_MASTER",    "Binge Master",                 "Assistiu 30 séries. Sono? Nunca ouvi falar.",                 "🛋️", 300, "SERIES_ASSISTIDAS", 30);
        upsertConquista("DUREZA_TOTAL",    "Dureza Total",                 "Dropou 30 itens. A vida é curta pra mídia ruim.",             "🗑️", 150, "TOTAL_DROPADOS",    30);
        upsertConquista("ARBITRO_SUPREMO", "Árbitro Supremo",              "Deu nota 10 para 25 itens. Seu critério é impecável.",        "🎖️", 250, "NOTA10_TOTAL",      25);
        upsertConquista("EXPANSAO_TOTAL",  "Expansão Total",               "Cadastrou 100 itens no backlog. O acervo cresce!",            "📚", 150, "TOTAL_ITENS",       100);
    }

    private void upsertConquista(String chave, String nome, String descricao,
                                  String icone, int xp,
                                  String criterioTipo, Integer criterioValor) {
        conquistaRepository.findByChave(chave).ifPresentOrElse(
            existing -> {
                // Atualiza o critério se ainda estava nulo ou era MANUAL (legado)
                boolean changed = false;
                if (existing.getCriterioTipo() == null || "MANUAL".equals(existing.getCriterioTipo())) {
                    existing.setCriterioTipo(criterioTipo);
                    changed = true;
                }
                if (existing.getCriterioValor() == null && criterioValor != null) {
                    existing.setCriterioValor(criterioValor);
                    changed = true;
                }
                if (changed) conquistaRepository.save(existing);
            },
            () -> {
                Conquista c = new Conquista();
                c.setChave(chave);
                c.setNome(nome);
                c.setDescricao(descricao);
                c.setIcone(icone);
                c.setXp(xp);
                c.setCriterioTipo(criterioTipo);
                c.setCriterioValor(criterioValor);
                conquistaRepository.save(c);
            }
        );
    }

    // ---------------------------------------------------------------
    // Verificação automática — chamada após cadastrar/editar item,
    // ao criar share link ou ao usar a IA.
    // ---------------------------------------------------------------

    @Async
    @Transactional
    public java.util.concurrent.CompletableFuture<List<ConquistaDesbloqueadaDTO>> verificarConquistas(User user) {
        List<ConquistaDesbloqueadaDTO> novas = new ArrayList<>();

        // Coleta contadores uma única vez
        List<Object[]> porTipo   = itemRepository.countItensPorTipo(user.getId());
        List<Object[]> porStatus = itemRepository.countItensPorStatus(user.getId());
        List<Object[]> porNota   = itemRepository.countItensPorNota(user.getId());

        Map<String, Long> countTipo   = toMap(porTipo);
        Map<String, Long> countStatus = toMap(porStatus);
        Map<Double, Long> countNota   = toMapDouble(porNota);

        long totalItens = countTipo.values().stream().mapToLong(Long::longValue).sum();
        long totalConcluidos = countStatus.getOrDefault("Zerado / Assistido", 0L)
                             + countStatus.getOrDefault("concluido", 0L)
                             + countStatus.getOrDefault("Zerado", 0L)
                             + countStatus.getOrDefault("Assistido", 0L);
        long totalDropados = countStatus.getOrDefault("Dropado", 0L)
                           + countStatus.getOrDefault("dropado", 0L);

        long jogosZerados     = concluidosDeTipo(user, "Jogo");
        long filmesAssistidos = concluidosDeTipo(user, "Filme");
        long seriesAssistidas = concluidosDeTipo(user, "Série");
        long nota10Total      = countNota.getOrDefault(10.0, 0L);
        long filmeNota10      = itemRepository.countByUserAndTipoAndNota(user, "Filme", 10.0);
        long jogosNota10      = itemRepository.countByUserAndTipoAndNota(user, "Jogo",  10.0);

        // Detecta TMDB: itens deste usuário com URL do themoviedb.org
        long itensComCapaTmdb = itemRepository.findByUser(user).stream()
            .filter(item -> item.getImagemUrl() != null && item.getImagemUrl().contains("themoviedb.org"))
            .count();

        // Detecta share link: quantos tokens este usuário já gerou
        long shareLinksGerados = shareTokenRepository.findByUser(user).size();

        // Itera TODAS as conquistas (fixas + criadas pelo admin)
        for (Conquista c : conquistaRepository.findAll()) {
            if (userConquistaRepository.existsByUserAndConquista_Chave(user, c.getChave())) {
                continue; // já possui
            }

            String tipo = c.getCriterioTipo();
            if (tipo == null || tipo.equalsIgnoreCase("MANUAL")) continue;

            // AI_USADA é detectada no ponto de disparo (desbloquearEvento),
            // não aqui — pois não temos como consultar o histórico de uso da IA no banco.
            if (tipo.equalsIgnoreCase("AI_USADA")) continue;

            int limiar = c.getCriterioValor() != null ? c.getCriterioValor() : 1;

            boolean atende = switch (tipo.toUpperCase()) {
                case "TOTAL_ITENS"       -> totalItens        >= limiar;
                case "TOTAL_CONCLUIDOS"  -> totalConcluidos   >= limiar;
                case "TOTAL_DROPADOS"    -> totalDropados      >= limiar;
                case "JOGOS_ZERADOS"     -> jogosZerados       >= limiar;
                case "FILMES_ASSISTIDOS" -> filmesAssistidos   >= limiar;
                case "SERIES_ASSISTIDAS" -> seriesAssistidas   >= limiar;
                case "NOTA10_FILMES"     -> filmeNota10        >= limiar;
                case "NOTA10_JOGOS"      -> jogosNota10        >= limiar;
                case "NOTA10_TOTAL"      -> nota10Total        >= limiar;
                case "TMDB_CAPA"         -> itensComCapaTmdb   >= limiar;
                case "SHARE_LINK_CRIADO" -> shareLinksGerados  >= limiar;
                default                  -> false;
            };

            if (atende) registrarDesbloqueio(user, c, novas);
        }

        return java.util.concurrent.CompletableFuture.completedFuture(novas);
    }

    /*
     * Dispara o desbloqueio de uma conquista vinculada a um evento pontual.
     *
     * Retorna o DTO se a conquista foi desbloqueada agora, ou null se o usuário
     * já a possuía.
     */
    @Transactional
    public ConquistaDesbloqueadaDTO desbloquearEvento(User user, String chave) {
        if (userConquistaRepository.existsByUserAndConquista_Chave(user, chave)) return null;
        Conquista c = conquistaRepository.findByChave(chave).orElse(null);
        if (c == null) return null;
        List<ConquistaDesbloqueadaDTO> lista = new ArrayList<>();
        registrarDesbloqueio(user, c, lista);
        return lista.isEmpty() ? null : lista.get(0);
    }

    /*
     * Concede uma conquista manualmente a um usuário (ação do admin).
     * Idempotente: não faz nada se o usuário já tiver a conquista.
     */
    @Transactional
    public boolean concederConquistaAdmin(User user, Long conquistaId) {
        Conquista c = conquistaRepository.findById(conquistaId)
                .orElseThrow(() -> new RuntimeException("Conquista não encontrada"));
        if (userConquistaRepository.existsByUserAndConquista_Chave(user, c.getChave())) {
            return false; // já tinha
        }
        userConquistaRepository.save(new UserConquista(user, c));
        return true;
    }

    /*
     * Revoga uma conquista de um usuário (ação administrativa).
     * Remove o registro UserConquista e retorna o XP que foi deduzido,
     * para que o admin veja o impacto no feedback.
     * Retorna -1 se o usuário não possuía a conquista.
     */
    @Transactional
    public int revogarConquistaAdmin(User user, Long conquistaId) {
        return userConquistaRepository.findByUserAndConquista_Id(user, conquistaId)
                .map(uc -> {
                    int xpDeduzido = uc.getConquista().getXp();
                    userConquistaRepository.delete(uc);
                    return xpDeduzido;
                })
                .orElse(-1);
    }

    // --- Helpers ---

    private void registrarDesbloqueio(User user, Conquista c, List<ConquistaDesbloqueadaDTO> novas) {
        userConquistaRepository.save(new UserConquista(user, c));
        novas.add(new ConquistaDesbloqueadaDTO(c.getChave(), c.getNome(), c.getDescricao(), c.getIcone(), c.getXp()));
    }

    private Map<String, Long> toMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(r -> (String) r[0], r -> (Long) r[1]));
    }

    private Map<Double, Long> toMapDouble(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(r -> (Double) r[0], r -> (Long) r[1]));
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

    // XP necessário para completar o nível N: 100 * N
    // Nível 1: 0–100 XP | Nível 2: 100–300 XP | Nível 3: 300–600 XP ...
    public int calcularNivel(int xpTotal) {
        if (xpTotal <= 0) return 1;
        int nivel = 1;
        int xpAcumulado = 0;
        while (true) {
            int xpParaSubir = 100 * nivel;
            if (xpTotal < xpAcumulado + xpParaSubir) break;
            xpAcumulado += xpParaSubir;
            nivel++;
        }
        return nivel;
    }

    // XP total necessário para CHEGAR ao início do nível N
    public int xpBaseDoNivel(int nivel) {
        int total = 0;
        for (int n = 1; n < nivel; n++) total += 100 * n;
        return total;
    }

    // XP necessário para avançar do nível atual para o próximo
    public int xpParaProximoNivel(int nivel) {
        return 100 * nivel;
    }

    public List<UserConquista> listarConquistasDoUsuario(User user) {
        return userConquistaRepository.findByUser(user);
    }

    public List<Conquista> listarTodasConquistas() {
        return conquistaRepository.findAll();
    }

    @Transactional
    public void deletarConquistasDoUsuario(User user) {
        userConquistaRepository.deleteByUser(user);
    }
}