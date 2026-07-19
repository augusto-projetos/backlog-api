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
    //   HORAS_FILMES         - soma de duracaoMinutos dos filmes concluídos >= criterioValor (em MINUTOS)
    //   HORAS_JOGOS_TOTAL    - soma de minutosJogados de TODOS os jogos cadastrados >= criterioValor (em MINUTOS)
    //   HORAS_JOGO_UNICO     - maior minutosJogados entre os jogos cadastrados (1 jogo só) >= criterioValor (em MINUTOS)
    //   HORAS_SERIES         - soma de duracaoTotalMinutos das séries concluídas (Assistido) >= criterioValor (em MINUTOS)
    //   EPISODIOS_TOTAL      - soma de episodioAtual de TODAS as séries cadastradas >= criterioValor (em EPISÓDIOS)
    //   EPISODIOS_SERIE_UNICA- maior episodioAtual entre as séries cadastradas (1 série só) >= criterioValor (em EPISÓDIOS)
    //   MANUAL               - concedida manualmente pelo admin
    //
    // IMPORTANTE: para os critérios de HORAS_*, criterioValor é sempre em MINUTOS
    // (ex.: 10 horas = 600). Isso porque Item.duracaoMinutos e Item.minutosJogados
    // são armazenados em minutos.
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

        // -----------------------------------------------------------
        // Conquistas de HORAS assistidas em filmes concluídos
        // -----------------------------------------------------------
        upsertConquista("HORAS_FILME_10",  "Pipoca Quentinha",             "Acumulou 10 horas assistindo filmes concluídos.",             "🍿", 60,  "HORAS_FILMES", h(10));
        upsertConquista("HORAS_FILME_25",  "Sessão da Tarde",              "Acumulou 25 horas assistindo filmes concluídos.",             "📽️", 80,  "HORAS_FILMES", h(25));
        upsertConquista("HORAS_FILME_50",  "Fim de Semana de Cinema",      "Acumulou 50 horas assistindo filmes concluídos.",             "🎦", 100, "HORAS_FILMES", h(50));
        upsertConquista("HORAS_FILME_100", "Cinéfilo de Carteirinha",      "Acumulou 100 horas assistindo filmes concluídos.",            "🎟️", 130, "HORAS_FILMES", h(100));
        upsertConquista("HORAS_FILME_200", "Maratonista das Telonas",      "Acumulou 200 horas assistindo filmes concluídos.",            "🏆", 160, "HORAS_FILMES", h(200));
        upsertConquista("HORAS_FILME_350", "Morador do Cinema",            "Acumulou 350 horas assistindo filmes concluídos.",            "🎬", 200, "HORAS_FILMES", h(350));
        upsertConquista("HORAS_FILME_500", "Lenda das Telonas",            "Acumulou 500 horas assistindo filmes concluídos.",            "🌟", 250, "HORAS_FILMES", h(500));

        // -----------------------------------------------------------
        // Conquistas de HORAS totais cadastradas em jogos
        // -----------------------------------------------------------
        upsertConquista("HORAS_JOGO_TOTAL_20",   "Recruta Gamer",                 "Cadastrou 20 horas jogadas no total.",                        "🎮", 70,  "HORAS_JOGOS_TOTAL", h(20));
        upsertConquista("HORAS_JOGO_TOTAL_50",   "Grinder Iniciante",             "Cadastrou 50 horas jogadas no total.",                        "⏫", 90,  "HORAS_JOGOS_TOTAL", h(50));
        upsertConquista("HORAS_JOGO_TOTAL_100",  "Cem Por Cento Dedicado",        "Cadastrou 100 horas jogadas no total.",                       "💯", 120, "HORAS_JOGOS_TOTAL", h(100));
        upsertConquista("HORAS_JOGO_TOTAL_250",  "Veterano dos Consoles",         "Cadastrou 250 horas jogadas no total.",                       "🕹️", 150, "HORAS_JOGOS_TOTAL", h(250));
        upsertConquista("HORAS_JOGO_TOTAL_500",  "Mestre do Save Point",          "Cadastrou 500 horas jogadas no total.",                       "💾", 200, "HORAS_JOGOS_TOTAL", h(500));
        upsertConquista("HORAS_JOGO_TOTAL_1000", "Rank Diamante da Vida Real",    "Cadastrou 1000 horas jogadas no total.",                      "💎", 280, "HORAS_JOGOS_TOTAL", h(1000));
        upsertConquista("HORAS_JOGO_TOTAL_2000", "NPC do Próprio Backlog",        "Cadastrou 2000 horas jogadas no total.",                      "🤖", 350, "HORAS_JOGOS_TOTAL", h(2000));

        // -----------------------------------------------------------
        // Conquistas de HORAS investidas em um ÚNICO jogo
        // -----------------------------------------------------------
        upsertConquista("HORAS_JOGO_UNICO_10",  "Só Mais Uma Partida",           "Registrou 10 horas jogadas em um único jogo.",                "🕐", 60,  "HORAS_JOGO_UNICO", h(10));
        upsertConquista("HORAS_JOGO_UNICO_25",  "Investimento Sério",            "Registrou 25 horas jogadas em um único jogo.",                "📈", 80,  "HORAS_JOGO_UNICO", h(25));
        upsertConquista("HORAS_JOGO_UNICO_50",  "Casamento com o Jogo",          "Registrou 50 horas jogadas em um único jogo.",                "💍", 110, "HORAS_JOGO_UNICO", h(50));
        upsertConquista("HORAS_JOGO_UNICO_100", "Cem Horas, Uma Vida",           "Registrou 100 horas jogadas em um único jogo.",               "⏳", 150, "HORAS_JOGO_UNICO", h(100));
        upsertConquista("HORAS_JOGO_UNICO_200", "Platina de Suor e Lágrimas",    "Registrou 200 horas jogadas em um único jogo.",               "🏅", 200, "HORAS_JOGO_UNICO", h(200));
        upsertConquista("HORAS_JOGO_UNICO_500", "Isso Já Não É Hobby, É Residência", "Registrou 500 horas jogadas em um único jogo.",           "🏠", 280, "HORAS_JOGO_UNICO", h(500));

        // -----------------------------------------------------------
        // Conquistas de HORAS assistidas em séries concluídas
        // -----------------------------------------------------------
        upsertConquista("HORAS_SERIE_10",  "Episódio Piloto",              "Acumulou 10 horas assistindo séries concluídas.",             "📺", 60,  "HORAS_SERIES", h(10));
        upsertConquista("HORAS_SERIE_25",  "Nas Primeiras Temporadas",     "Acumulou 25 horas assistindo séries concluídas.",             "📼", 80,  "HORAS_SERIES", h(25));
        upsertConquista("HORAS_SERIE_50",  "Fim de Semana de Série",       "Acumulou 50 horas assistindo séries concluídas.",             "🛋️", 100, "HORAS_SERIES", h(50));
        upsertConquista("HORAS_SERIE_100", "Assinante Fiel",               "Acumulou 100 horas assistindo séries concluídas.",            "📡", 130, "HORAS_SERIES", h(100));
        upsertConquista("HORAS_SERIE_200", "Maratonista de Streaming",     "Acumulou 200 horas assistindo séries concluídas.",            "🏆", 160, "HORAS_SERIES", h(200));
        upsertConquista("HORAS_SERIE_350", "Sofá Vitalício",               "Acumulou 350 horas assistindo séries concluídas.",            "🛏️", 200, "HORAS_SERIES", h(350));
        upsertConquista("HORAS_SERIE_500", "Lenda do Streaming",           "Acumulou 500 horas assistindo séries concluídas.",            "🌟", 250, "HORAS_SERIES", h(500));

        // -----------------------------------------------------------
        // Conquistas de EPISÓDIOS somando TODAS as séries cadastradas
        // -----------------------------------------------------------
        upsertConquista("EPISODIOS_TOTAL_50",   "Primeiros Episódios",           "Registrou 50 episódios assistidos no total.",                 "🎬", 70,  "EPISODIOS_TOTAL", 50);
        upsertConquista("EPISODIOS_TOTAL_150",  "Colecionador de Episódios",     "Registrou 150 episódios assistidos no total.",                "📚", 100, "EPISODIOS_TOTAL", 150);
        upsertConquista("EPISODIOS_TOTAL_300",  "Vidão de Streaming",            "Registrou 300 episódios assistidos no total.",                "🎞️", 140, "EPISODIOS_TOTAL", 300);
        upsertConquista("EPISODIOS_TOTAL_500",  "Enciclopédia de Séries",        "Registrou 500 episódios assistidos no total.",                "📖", 180, "EPISODIOS_TOTAL", 500);
        upsertConquista("EPISODIOS_TOTAL_1000", "Mil Episódios, Zero Arrependimentos", "Registrou 1000 episódios assistidos no total.",         "🏅", 250, "EPISODIOS_TOTAL", 1000);

        // -----------------------------------------------------------
        // Conquistas de EPISÓDIOS assistidos em uma ÚNICA série (maratona)
        // -----------------------------------------------------------
        upsertConquista("EPISODIOS_SERIE_UNICA_20",  "Vício Declarado",             "Assistiu 20 episódios de uma mesma série.",             "😵", 70,  "EPISODIOS_SERIE_UNICA", 20);
        upsertConquista("EPISODIOS_SERIE_UNICA_50",  "Binge Sem Freio",             "Assistiu 50 episódios de uma mesma série.",             "🛑", 100, "EPISODIOS_SERIE_UNICA", 50);
        upsertConquista("EPISODIOS_SERIE_UNICA_100", "Cem Episódios, Uma Obsessão", "Assistiu 100 episódios de uma mesma série.",             "💫", 150, "EPISODIOS_SERIE_UNICA", 100);
        upsertConquista("EPISODIOS_SERIE_UNICA_200", "Essa Série Virou Minha Vida", "Assistiu 200 episódios de uma mesma série.",             "🧠", 220, "EPISODIOS_SERIE_UNICA", 200);
    }

    // Converte horas para minutos, usado para deixar o seed mais legível
    private static int h(int horas) {
        return horas * 60;
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

    @Transactional
    public List<ConquistaDesbloqueadaDTO> verificarConquistas(User user) {
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

        // Minutos assistidos de filmes concluídos (para conquistas HORAS_FILMES)
        long minutosFilmesAssistidos = itemRepository.sumDuracaoMinutosByUserAndTipoAndStatusIn(
            user, "Filme",
            List.of("Zerado / Assistido", "concluido", "Zerado", "Assistido")
        );

        // Minutos jogados somando TODOS os jogos cadastrados (para conquistas HORAS_JOGOS_TOTAL)
        long minutosJogosTotal = itemRepository.sumMinutosJogadosByUserAndTipo(user, "Jogo");

        // Maior quantidade de minutos jogados em um único jogo (para conquistas HORAS_JOGO_UNICO)
        long minutosJogoUnicoMax = itemRepository.maxMinutosJogadosByUserAndTipo(user, "Jogo");

        // Minutos assistidos de séries concluídas (para conquistas HORAS_SERIES)
        long minutosSeriesAssistidas = itemRepository.sumDuracaoTotalMinutosByUserAndTipoAndStatusIn(
            user, "Série",
            List.of("Zerado / Assistido", "concluido", "Zerado", "Assistido")
        );

        // Soma de episódios de TODAS as séries cadastradas (para conquistas EPISODIOS_TOTAL)
        long episodiosTotal = itemRepository.sumEpisodiosByUserAndTipo(user, "Série");

        // Maior quantidade de episódios em uma única série (para conquistas EPISODIOS_SERIE_UNICA)
        long episodiosSerieUnicaMax = itemRepository.maxEpisodiosByUserAndTipo(user, "Série");

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
                case "HORAS_FILMES"      -> minutosFilmesAssistidos >= limiar;
                case "HORAS_JOGOS_TOTAL" -> minutosJogosTotal       >= limiar;
                case "HORAS_JOGO_UNICO"  -> minutosJogoUnicoMax     >= limiar;
                case "HORAS_SERIES"      -> minutosSeriesAssistidas >= limiar;
                case "EPISODIOS_TOTAL"       -> episodiosTotal        >= limiar;
                case "EPISODIOS_SERIE_UNICA" -> episodiosSerieUnicaMax >= limiar;
                default                  -> false;
            };

            if (atende) registrarDesbloqueio(user, c, novas);
        }

        return novas;
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
