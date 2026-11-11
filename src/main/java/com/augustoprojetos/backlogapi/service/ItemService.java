package com.augustoprojetos.backlogapi.service;

import com.augustoprojetos.backlogapi.dto.AtualizacaoDuracaoDTO;
import com.augustoprojetos.backlogapi.dto.ConquistaDesbloqueadaDTO;
import com.augustoprojetos.backlogapi.dto.ItemRequest;
import com.augustoprojetos.backlogapi.entity.Item;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.repository.ItemRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/*
 * Regras de negócio dos itens do backlog.
 *
 * Cada ação do usuário roda em uma transação curta (item + logs), com um único commit.
 * A verificação de conquistas roda numa segunda transação, separada de propósito: se ela
 * falhar, o item já salvo NÃO é desfeito.
 */
@Service
public class ItemService {

    private static final Logger log = LoggerFactory.getLogger(ItemService.class);

    private static final String TIPO_FILME = "Filme";
    private static final String TIPO_SERIE = "Série";
    private static final String TIPO_JOGO = "Jogo";
    private static final String STATUS_BACKLOG = "Backlog";
    private static final String STATUS_ASSISTIDO = "Assistido";

    // Formato aceito para a chave de idempotência (UUID ou similar): evita lixo/valores gigantes no banco
    private static final Pattern CHAVE_IDEMPOTENCIA_VALIDA = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");
    // grupoId vem do cliente em PUT /itens/{id}/duracao; a coluna comporta 36 caracteres (UUID)
    private static final Pattern GRUPO_ID_VALIDO = Pattern.compile("^[A-Za-z0-9-]{1,36}$");

    private static final int TIMEOUT_SUGESTOES_SEGUNDOS = 8;

    /** Resultado das ações que podem falhar por regra de negócio (o controller traduz em HTTP). */
    public enum Status { OK, NAO_ENCONTRADO, PROIBIDO, REQUISICAO_INVALIDA }

    public record ResultadoItem(Status status, String erro, Item item, List<ConquistaDesbloqueadaDTO> conquistas) {

        static ResultadoItem ok(Item item, List<ConquistaDesbloqueadaDTO> conquistas) {
            return new ResultadoItem(Status.OK, null, item, conquistas);
        }

        static ResultadoItem falha(Status status, String erro) {
            return new ResultadoItem(status, erro, null, List.of());
        }

        public boolean sucesso() {
            return status == Status.OK;
        }
    }

    /** Resultado do salvamento em lote de durações. */
    public record ResultadoLote(int atualizados, List<ConquistaDesbloqueadaDTO> conquistas) {}

    // Foto dos campos ANTES da edição: a entidade gerenciada é alterada no lugar, então é
    // preciso guardar o estado anterior para detectar o que mudou (e logar o evento certo).
    private record Estado(String titulo, String tipo, String status, Double nota, String resenha,
                          String imagemUrl, Integer duracaoMinutos, Integer minutosJogados,
                          Integer temporadaAtual, Integer episodioAtual, Integer duracaoTotalMinutos) {

        static Estado de(Item i) {
            return new Estado(i.getTitulo(), i.getTipo(), i.getStatus(), i.getNota(), i.getResenha(),
                    i.getImagemUrl(), i.getDuracaoMinutos(), i.getMinutosJogados(),
                    i.getTemporadaAtual(), i.getEpisodioAtual(), i.getDuracaoTotalMinutos());
        }
    }

    private final ItemRepository itemRepository;
    private final TmdbService tmdbService;
    private final ConquistaService conquistaService;
    private final AtividadeLogService atividadeLogService;
    private final TransactionTemplate tx;

    // Pool próprio e limitado para as consultas ao TMDB (I/O bloqueante não deve ir pro ForkJoinPool comum)
    private final ExecutorService executorTmdb = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "tmdb-duracao");
        thread.setDaemon(true);
        return thread;
    });

    public ItemService(ItemRepository itemRepository,
                       TmdbService tmdbService,
                       ConquistaService conquistaService,
                       AtividadeLogService atividadeLogService,
                       PlatformTransactionManager transactionManager) {
        this.itemRepository = itemRepository;
        this.tmdbService = tmdbService;
        this.conquistaService = conquistaService;
        this.atividadeLogService = atividadeLogService;
        this.tx = new TransactionTemplate(transactionManager);
    }

    @PreDestroy
    void encerrarExecutor() {
        executorTmdb.shutdownNow();
    }

    // ------------------------------------------------------------------
    // Consultas simples
    // ------------------------------------------------------------------

    public List<Item> listar(User user) {
        return itemRepository.findByUser(user);
    }

    public Optional<Item> buscarPorId(Long id) {
        return itemRepository.findById(id);
    }

    public boolean pertenceA(Item item, User user) {
        return item.getUser() != null && user != null && Objects.equals(item.getUser().getId(), user.getId());
    }

    /** Filmes do usuário sem duração (a filtragem acontece no banco, não em memória). */
    public List<Item> listarFilmesSemDuracao(User user) {
        return itemRepository.findByUserAndTipoIgnoreCaseAndDuracaoMinutosIsNull(user, TIPO_FILME);
    }

    // ------------------------------------------------------------------
    // 1. Cadastrar
    // ------------------------------------------------------------------

    public ResultadoItem cadastrar(ItemRequest requisicao, String idempotencyKey, User user) {
        // Idempotência: se essa mesma requisição já foi processada (duplo clique, retry de
        // rede/proxy, aba duplicada), devolvemos o item que já existe.
        String chave = normalizarChaveIdempotencia(idempotencyKey);
        if (chave != null) {
            Optional<Item> jaCadastrado = itemRepository.findByUserAndClientRequestId(user, chave);
            if (jaCadastrado.isPresent()) {
                log.info("Cadastro repetido ignorado (usuário {}, chave {})", user.getId(), chave);
                return ResultadoItem.ok(jaCadastrado.get(), List.of());
            }
        }

        // O id nunca vem do cliente: a entidade nasce sempre nova.
        Item novo = requisicao.paraNovaEntidade();
        novo.setUser(user);
        novo.setClientRequestId(chave);
        limparProgressoSerieSeBacklog(novo);

        // Todo evento gerado por essa mesma requisição compartilha o grupoId.
        String grupoId = atividadeLogService.novoGrupo();

        Item salvo;
        try {
            salvo = tx.execute(status -> {
                Item gravado = itemRepository.save(novo);
                atividadeLogService.registrarItemAdicionado(user, gravado, grupoId);
                return gravado;
            });
        } catch (DataIntegrityViolationException e) {
            // Duas requisições com a mesma chave chegaram ao mesmo tempo: a outra gravou primeiro
            // e o índice único barrou esta.
            Optional<Item> venceuCorrida = chave != null
                    ? itemRepository.findByUserAndClientRequestId(user, chave)
                    : Optional.empty();
            if (venceuCorrida.isPresent()) {
                log.info("Cadastro concorrente ignorado (usuário {}, chave {})", user.getId(), chave);
                return ResultadoItem.ok(venceuCorrida.get(), List.of());
            }
            throw e;
        }

        return ResultadoItem.ok(salvo, verificarConquistas(user, grupoId));
    }

    // ------------------------------------------------------------------
    // 2. Editar
    // ------------------------------------------------------------------

    public ResultadoItem atualizar(Long id, ItemRequest requisicao, User user) {
        String grupoId = atividadeLogService.novoGrupo();

        ResultadoItem fase1 = tx.execute(status -> {
            Item item = itemRepository.findById(id).orElse(null);
            if (item == null) {
                // Evita criar um registro "fantasma" com ID manual.
                return ResultadoItem.falha(Status.NAO_ENCONTRADO, null);
            }
            if (!pertenceA(item, user)) {
                return ResultadoItem.falha(Status.PROIBIDO, "Acesso negado.");
            }

            Estado anterior = Estado.de(item);

            // Altera a entidade já gerenciada: o Hibernate gera um único UPDATE no commit
            // (antes: findById + save de objeto solto = SELECT extra do merge + UPDATE).
            // Usuário e chave de idempotência do cadastro original são preservados.
            requisicao.aplicarEm(item);
            limparProgressoSerieSeBacklog(item);

            registrarMudancas(user, anterior, item, grupoId);
            return ResultadoItem.ok(item, List.of());
        });

        if (fase1 == null || !fase1.sucesso()) {
            return fase1;
        }
        return ResultadoItem.ok(fase1.item(), verificarConquistas(user, grupoId));
    }

    // Detecta o que mudou e registra o evento mais específico de cada coisa
    private void registrarMudancas(User user, Estado anterior, Item salvo, String grupoId) {
        boolean statusMudou = !Objects.equals(anterior.status(), salvo.getStatus());
        boolean notaMudou = !Objects.equals(anterior.nota(), salvo.getNota());
        boolean resenhaMudou = !Objects.equals(anterior.resenha(), salvo.getResenha());
        boolean duracaoMudou = !Objects.equals(anterior.duracaoMinutos(), salvo.getDuracaoMinutos())
                || !Objects.equals(anterior.minutosJogados(), salvo.getMinutosJogados())
                || !Objects.equals(anterior.duracaoTotalMinutos(), salvo.getDuracaoTotalMinutos());
        boolean progressoMudou = (!Objects.equals(anterior.temporadaAtual(), salvo.getTemporadaAtual())
                || !Objects.equals(anterior.episodioAtual(), salvo.getEpisodioAtual()))
                && !STATUS_BACKLOG.equalsIgnoreCase(salvo.getStatus());

        if (statusMudou) {
            atividadeLogService.registrarStatusAlterado(user, salvo, anterior.status(), grupoId);
        }
        if (notaMudou) {
            atividadeLogService.registrarNotaAlterada(user, salvo, anterior.nota(), grupoId);
        }
        if (resenhaMudou) {
            boolean resenhaEraVazia = anterior.resenha() == null || anterior.resenha().isBlank();
            boolean temResenhaAgora = salvo.getResenha() != null && !salvo.getResenha().isBlank();
            if (resenhaEraVazia && temResenhaAgora) {
                atividadeLogService.registrarResenhaAdicionada(user, salvo, grupoId);
            } else if (temResenhaAgora) {
                atividadeLogService.registrarResenhaEditada(user, salvo, grupoId);
            }
        }
        if (duracaoMudou) {
            atividadeLogService.registrarDuracaoAlterada(user, salvo, formatarDetalheDuracao(anterior, salvo), grupoId);
        }
        if (progressoMudou) {
            atividadeLogService.registrarProgressoSerieAtualizado(
                    user, salvo, anterior.temporadaAtual(), anterior.episodioAtual(), grupoId);
        }

        // Campos "genéricos" que não têm um evento dedicado (título, capa, tipo)
        List<String> camposAlterados = new ArrayList<>();
        if (!Objects.equals(anterior.titulo(), salvo.getTitulo())) camposAlterados.add("Título");
        if (!Objects.equals(anterior.imagemUrl(), salvo.getImagemUrl())) camposAlterados.add("Capa");
        if (!Objects.equals(anterior.tipo(), salvo.getTipo())) camposAlterados.add("Tipo");

        boolean algumEventoEspecificoRegistrado = statusMudou || notaMudou || resenhaMudou || duracaoMudou || progressoMudou;

        if (!camposAlterados.isEmpty()) {
            atividadeLogService.registrarItemEditado(user, salvo, camposAlterados, grupoId);
        } else if (!algumEventoEspecificoRegistrado) {
            // Nada que a gente rastreia mudou de fato, mantemos um rastro genérico.
            atividadeLogService.registrarItemEditado(user, salvo, grupoId);
        }
    }

    // Monta o "antes → depois" da duração, olhando o campo certo conforme o tipo
    private String formatarDetalheDuracao(Estado anterior, Item salvo) {
        Integer valorAnterior;
        Integer valorNovo;

        if (TIPO_JOGO.equalsIgnoreCase(salvo.getTipo())) {
            valorAnterior = anterior.minutosJogados();
            valorNovo = salvo.getMinutosJogados();
        } else if (TIPO_SERIE.equalsIgnoreCase(salvo.getTipo())) {
            valorAnterior = anterior.duracaoTotalMinutos();
            valorNovo = salvo.getDuracaoTotalMinutos();
        } else {
            valorAnterior = anterior.duracaoMinutos();
            valorNovo = salvo.getDuracaoMinutos();
        }

        return formatarMinutos(valorAnterior) + " → " + formatarMinutos(valorNovo);
    }

    private static String formatarMinutos(Integer minutos) {
        return minutos != null ? minutos + " min" : "—";
    }

    // ------------------------------------------------------------------
    // 3. Deletar
    // ------------------------------------------------------------------

    public void deletar(Long id, User user) {
        // Sem usuário autenticado ninguém apaga nada (antes, user nulo pulava a checagem de dono).
        if (user == null) {
            return;
        }
        tx.executeWithoutResult(status -> {
            Item item = itemRepository.findById(id).orElse(null);
            if (item == null || !pertenceA(item, user)) {
                return;
            }
            atividadeLogService.registrarItemRemovido(user, item);
            itemRepository.delete(item);
        });
    }

    // ------------------------------------------------------------------
    // 4. Duração de filmes
    // ------------------------------------------------------------------

    /** Busca no TMDB, em paralelo e com um prazo total, uma sugestão de duração para cada filme pedido. */
    public List<Map<String, Object>> sugerirDuracoes(List<Long> ids, User user) {
        List<Item> filmes = itemRepository.findAllById(ids).stream()
                .filter(i -> pertenceA(i, user))
                .filter(i -> TIPO_FILME.equalsIgnoreCase(i.getTipo()))
                .toList();

        List<CompletableFuture<Integer>> buscas = filmes.stream()
                .map(filme -> CompletableFuture.supplyAsync(() -> buscarDuracaoSemFalhar(filme.getTitulo()), executorTmdb))
                .toList();

        // Um único prazo para o conjunto todo (antes cada filme esperava até 8s em sequência)
        try {
            CompletableFuture.allOf(buscas.toArray(new CompletableFuture[0]))
                    .get(TIMEOUT_SUGESTOES_SEGUNDOS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // Timeout/erro: quem não terminou fica sem sugestão e o usuário preenche na mão
            log.debug("Sugestões de duração incompletas: {}", e.getMessage());
        }

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (int i = 0; i < filmes.size(); i++) {
            CompletableFuture<Integer> busca = buscas.get(i);
            Integer minutos = null;
            if (busca.isDone() && !busca.isCompletedExceptionally() && !busca.isCancelled()) {
                minutos = busca.getNow(null);
            } else {
                busca.cancel(true);
            }
            // Mesmo sem sugestão, devolve "id" e "titulo"
            Map<String, Object> linha = new LinkedHashMap<>();
            linha.put("id", filmes.get(i).getId());
            linha.put("titulo", filmes.get(i).getTitulo());
            linha.put("minutosSugeridos", minutos);
            resultado.add(linha);
        }
        return resultado;
    }

    private Integer buscarDuracaoSemFalhar(String titulo) {
        try {
            return tmdbService.buscarDuracaoFilme(titulo);
        } catch (Exception e) {
            return null;
        }
    }

    /* Salva em lote só os filmes que o usuário confirmou: 1 SELECT + UPDATEs agrupados + 1 commit. */
    public ResultadoLote salvarDuracaoLote(List<AtualizacaoDuracaoDTO> atualizacoes, User user) {
        List<Long> ids = atualizacoes.stream()
                .map(AtualizacaoDuracaoDTO::id)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Integer atualizados = tx.execute(status -> {
            Map<Long, Item> itensPorId = itemRepository.findAllById(ids).stream()
                    .collect(Collectors.toMap(Item::getId, Function.identity()));

            int contador = 0;
            for (AtualizacaoDuracaoDTO dto : atualizacoes) {
                Item item = dto.id() != null ? itensPorId.get(dto.id()) : null;
                if (item != null
                        && pertenceA(item, user)
                        && TIPO_FILME.equalsIgnoreCase(item.getTipo())
                        && dto.duracaoMinutos() != null
                        && dto.duracaoMinutos() >= 0) {
                    item.setDuracaoMinutos(dto.duracaoMinutos()); // dirty checking grava no commit
                    contador++;
                }
            }
            atividadeLogService.registrarDuracaoLoteAtualizada(user, contador);
            return contador;
        });

        int total = atualizados != null ? atualizados : 0;
        List<ConquistaDesbloqueadaDTO> novas = total > 0
                ? verificarConquistas(user, atividadeLogService.novoGrupo())
                : List.of();
        return new ResultadoLote(total, novas);
    }

    /*
     * Salva a duração de um único filme.
     *
     * @param minutos null quando o valor enviado era inválido (ausente, não numérico ou negativo)
     */
    public ResultadoItem salvarDuracaoUnica(Long id, Integer minutos, String grupoIdRecebido, User user) {
        AtomicBoolean alterou = new AtomicBoolean(false);
        String grupoId = (grupoIdRecebido != null && GRUPO_ID_VALIDO.matcher(grupoIdRecebido).matches())
                ? grupoIdRecebido
                : atividadeLogService.novoGrupo();

        ResultadoItem fase1 = tx.execute(status -> {
            Item item = itemRepository.findById(id).orElse(null);
            if (item == null || !pertenceA(item, user)) {
                return ResultadoItem.falha(Status.PROIBIDO, null);
            }
            if (!TIPO_FILME.equalsIgnoreCase(item.getTipo()) || minutos == null) {
                return ResultadoItem.falha(Status.REQUISICAO_INVALIDA, null);
            }

            Integer duracaoAnterior = item.getDuracaoMinutos();
            item.setDuracaoMinutos(minutos);

            if (!Objects.equals(duracaoAnterior, minutos)) {
                alterou.set(true);
                String detalhe = formatarMinutos(duracaoAnterior) + " → " + formatarMinutos(minutos);
                atividadeLogService.registrarDuracaoAlterada(user, item, detalhe, grupoId);
            }
            return ResultadoItem.ok(item, List.of());
        });

        if (fase1 == null || !fase1.sucesso() || !alterou.get()) {
            return fase1;
        }
        return ResultadoItem.ok(fase1.item(), verificarConquistas(user, grupoId));
    }

    // ------------------------------------------------------------------
    // 5. +1 episódio
    // ------------------------------------------------------------------

    public ResultadoItem avancarEpisodio(Long id, User user) {
        String grupoId = atividadeLogService.novoGrupo();

        ResultadoItem fase1 = tx.execute(status -> {
            Item item = itemRepository.findById(id).orElse(null);
            if (item == null || !pertenceA(item, user)) {
                return ResultadoItem.falha(Status.PROIBIDO, null);
            }
            if (!TIPO_SERIE.equalsIgnoreCase(item.getTipo())) {
                return ResultadoItem.falha(Status.REQUISICAO_INVALIDA, "Este item não é uma série.");
            }
            // Enquanto a série estiver no Backlog, o progresso continua travado
            if (STATUS_BACKLOG.equalsIgnoreCase(item.getStatus())) {
                return ResultadoItem.falha(Status.REQUISICAO_INVALIDA,
                        "Tire a série do Backlog para começar a registrar episódios.");
            }
            // Série já concluída não avança mais episódio
            if (STATUS_ASSISTIDO.equalsIgnoreCase(item.getStatus())) {
                return ResultadoItem.falha(Status.REQUISICAO_INVALIDA, "Esta série já foi marcada como Assistida.");
            }

            Integer temporadaAnterior = item.getTemporadaAtual();
            Integer episodioAnterior = item.getEpisodioAtual();
            item.setEpisodioAtual((episodioAnterior != null ? episodioAnterior : 0) + 1);
            if (item.getTemporadaAtual() == null) {
                item.setTemporadaAtual(1);
            }

            atividadeLogService.registrarProgressoSerieAtualizado(user, item, temporadaAnterior, episodioAnterior, grupoId);
            return ResultadoItem.ok(item, List.of());
        });

        if (fase1 == null || !fase1.sucesso()) {
            return fase1;
        }
        return ResultadoItem.ok(fase1.item(), verificarConquistas(user, grupoId));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /*
     * Segunda transação: confere conquistas e registra as desbloqueadas na timeline.
     * Qualquer falha aqui é logada e engolida, para nunca quebrar a ação principal já gravada.
     */
    private List<ConquistaDesbloqueadaDTO> verificarConquistas(User user, String grupoId) {
        try {
            List<ConquistaDesbloqueadaDTO> novas = tx.execute(status -> {
                List<ConquistaDesbloqueadaDTO> desbloqueadas = conquistaService.verificarConquistas(user);
                desbloqueadas.forEach(c -> atividadeLogService.registrarConquistaDesbloqueada(
                        user, c.nome(), c.icone(), grupoId));
                return desbloqueadas;
            });
            return novas != null ? novas : List.of();
        } catch (Exception e) {
            log.warn("Falha ao verificar conquistas para o usuário {}: {}", user.getId(), e.getMessage(), e);
            return List.of();
        }
    }

    // Aceita só chaves no formato esperado; qualquer outra coisa é ignorada (cadastro segue sem idempotência)
    private String normalizarChaveIdempotencia(String chave) {
        if (chave == null) return null;
        String limpa = chave.trim();
        return CHAVE_IDEMPOTENCIA_VALIDA.matcher(limpa).matches() ? limpa : null;
    }

    // Enquanto a série está no Backlog, ela não tem progresso
    private void limparProgressoSerieSeBacklog(Item item) {
        boolean ehSerie = TIPO_SERIE.equalsIgnoreCase(item.getTipo());
        boolean emBacklog = STATUS_BACKLOG.equalsIgnoreCase(item.getStatus());
        if (ehSerie && emBacklog) {
            item.setTemporadaAtual(null);
            item.setEpisodioAtual(null);
        }
    }
}
