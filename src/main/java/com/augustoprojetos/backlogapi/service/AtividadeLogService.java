package com.augustoprojetos.backlogapi.service;

import com.augustoprojetos.backlogapi.dto.TimelineGrupoDTO;
import com.augustoprojetos.backlogapi.entity.AtividadeLog;
import com.augustoprojetos.backlogapi.entity.Item;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.repository.AtividadeLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AtividadeLogService {

    @Autowired
    private AtividadeLogRepository repository;

    // --- Agrupamento ---

    // Gera um novo ID de grupo.
    public String novoGrupo() {
        return UUID.randomUUID().toString();
    }

    // --- Métodos de registro ---
    // Cada um tem uma versão "solo" (sem grupoId, pra quando o evento é único
    // e não precisa ser agrupado com mais nada) e uma versão que recebe o
    // grupoId explicitamente.

    public void registrarContaCriada(User user) {
        salvar(AtividadeLog.of(user,
                "CONTA_CRIADA",
                "Criou a conta no Meu Backlog 🎉"));
    }

    public void registrarItemAdicionado(User user, Item item) {
        registrarItemAdicionado(user, item, novoGrupo());
    }

    public void registrarItemAdicionado(User user, Item item, String grupoId) {
        String tipo = item.getTipo();
        String emoji = emojiTipo(tipo);
        salvar(AtividadeLog.of(user,
                "ITEM_ADICIONADO",
                emoji + " Adicionou \"" + item.getTitulo() + "\" ao backlog",
                "Tipo: " + tipo + " • Status inicial: " + item.getStatus(),
                item.getTitulo()), grupoId);
    }

    public void registrarItemRemovido(User user, Item item) {
        registrarItemRemovido(user, item, novoGrupo());
    }

    public void registrarItemRemovido(User user, Item item, String grupoId) {
        String emoji = emojiTipo(item.getTipo());
        salvar(AtividadeLog.of(user,
                "ITEM_REMOVIDO",
                emoji + " Removeu \"" + item.getTitulo() + "\" do backlog",
                "Tipo: " + item.getTipo(),
                item.getTitulo()), grupoId);
    }

    public void registrarStatusAlterado(User user, Item item, String statusAnterior) {
        registrarStatusAlterado(user, item, statusAnterior, novoGrupo());
    }

    public void registrarStatusAlterado(User user, Item item, String statusAnterior, String grupoId) {
        String emoji = emojiStatus(item.getStatus());
        String detalhe = statusAnterior + " → " + item.getStatus();
        salvar(AtividadeLog.of(user,
                "STATUS_ALTERADO",
                emoji + " Mudou o status de \"" + item.getTitulo() + "\"",
                detalhe,
                item.getTitulo()), grupoId);
    }

    public void registrarNotaAlterada(User user, Item item, Double notaAnterior) {
        registrarNotaAlterada(user, item, notaAnterior, novoGrupo());
    }

    public void registrarNotaAlterada(User user, Item item, Double notaAnterior, String grupoId) {
        String detalhe = notaAnterior != null
                ? notaAnterior + " → " + item.getNota()
                : "Nota definida: " + item.getNota();
        salvar(AtividadeLog.of(user,
                "NOTA_ALTERADA",
                "⭐ Alterou a nota de \"" + item.getTitulo() + "\"",
                detalhe,
                item.getTitulo()), grupoId);
    }

    public void registrarResenhaAdicionada(User user, Item item) {
        registrarResenhaAdicionada(user, item, novoGrupo());
    }

    public void registrarResenhaAdicionada(User user, Item item, String grupoId) {
        salvar(AtividadeLog.of(user,
                "RESENHA_ADICIONADA",
                "✍️ Escreveu uma resenha para \"" + item.getTitulo() + "\"",
                null,
                item.getTitulo()), grupoId);
    }

    public void registrarResenhaEditada(User user, Item item) {
        registrarResenhaEditada(user, item, novoGrupo());
    }

    public void registrarResenhaEditada(User user, Item item, String grupoId) {
        salvar(AtividadeLog.of(user,
                "RESENHA_EDITADA",
                "📝 Editou a resenha de \"" + item.getTitulo() + "\"",
                null,
                item.getTitulo()), grupoId);
    }

    // 📺 Progresso de série avançado (seja pelo botão "+1" na Home, seja
    // editando manualmente temporada/episódio na tela de edição).
    public void registrarProgressoSerieAtualizado(User user, Item item,
                                                   Integer temporadaAnterior, Integer episodioAnterior) {
        registrarProgressoSerieAtualizado(user, item, temporadaAnterior, episodioAnterior, novoGrupo());
    }

    public void registrarProgressoSerieAtualizado(User user, Item item,
                                                   Integer temporadaAnterior, Integer episodioAnterior,
                                                   String grupoId) {
        String progressoAnterior = formatarProgressoSerie(temporadaAnterior, episodioAnterior);
        String progressoNovo = formatarProgressoSerie(item.getTemporadaAtual(), item.getEpisodioAtual());
        salvar(AtividadeLog.of(user,
                "PROGRESSO_SERIE_ATUALIZADO",
                "📺 Avançou o progresso de \"" + item.getTitulo() + "\"",
                progressoAnterior + " → " + progressoNovo,
                item.getTitulo()), grupoId);
    }

    // 🕒 Duração informada/alterada manualmente (filme, jogo ou série).
    public void registrarDuracaoAlterada(User user, Item item, String detalhe) {
        registrarDuracaoAlterada(user, item, detalhe, novoGrupo());
    }

    public void registrarDuracaoAlterada(User user, Item item, String detalhe, String grupoId) {
        String emoji = emojiTipo(item.getTipo());
        salvar(AtividadeLog.of(user,
                "DURACAO_ALTERADA",
                emoji + " Atualizou a duração de \"" + item.getTitulo() + "\"",
                detalhe,
                item.getTitulo()), grupoId);
    }

    // 🎬 Preenchimento em lote de duração de vários filmes de uma vez.
    public void registrarDuracaoLoteAtualizada(User user, int quantidade) {
        if (quantidade <= 0) return;
        salvar(AtividadeLog.of(user,
                "DURACAO_LOTE_ATUALIZADA",
                "🎬 Atualizou a duração de " + quantidade + " filme(s) em lote",
                null,
                null));
    }

    // Catch-all: usado só quando algo mudou mas não é nenhum dos casos
    // específicos acima (ex: título, capa ou tipo do item).
    public void registrarItemEditado(User user, Item item) {
        registrarItemEditado(user, item, null, novoGrupo());
    }

    public void registrarItemEditado(User user, Item item, String grupoId) {
        registrarItemEditado(user, item, null, grupoId);
    }

    public void registrarItemEditado(User user, Item item, List<String> camposAlterados, String grupoId) {
        String emoji = emojiTipo(item.getTipo());
        String detalhe = (camposAlterados != null && !camposAlterados.isEmpty())
                ? "Alterado: " + String.join(", ", camposAlterados)
                : null;
        salvar(AtividadeLog.of(user,
                "ITEM_EDITADO",
                emoji + " Editou \"" + item.getTitulo() + "\"",
                detalhe,
                item.getTitulo()), grupoId);
    }

    public void registrarConquistaDesbloqueada(User user, String nomeConquista, String icone) {
        registrarConquistaDesbloqueada(user, nomeConquista, icone, novoGrupo());
    }

    public void registrarConquistaDesbloqueada(User user, String nomeConquista, String icone, String grupoId) {
        salvar(AtividadeLog.of(user,
                "CONQUISTA_DESBLOQUEADA",
                (icone != null ? icone + " " : "🏆 ") + "Desbloqueou a conquista \"" + nomeConquista + "\"",
                null,
                null), grupoId);
    }

    public void registrarPerfilEditado(User user) {
        salvar(AtividadeLog.of(user,
                "PERFIL_EDITADO",
                "⚙️ Editou as informações do perfil"));
    }

    // --- Leitura ---

    // Timeline completa do usuário atual
    public List<AtividadeLog> buscarTimelineDoUsuario(User user) {
        return repository.findByUserOrderByCriadoEmDesc(user);
    }

    // Timeline de qualquer usuário (uso admin)
    public List<AtividadeLog> buscarTimelinePorUserId(Long userId) {
        return repository.findByUserIdOrderByCriadoEmDesc(userId);
    }

    // Agrupa a lista (já ordenada da mais recente pra mais antiga) em blocos
    // de eventos que compartilham o mesmo grupoId e estão em sequência
    public List<TimelineGrupoDTO> agruparParaTimeline(List<AtividadeLog> logs) {
        List<TimelineGrupoDTO> grupos = new ArrayList<>();
        List<AtividadeLog> bufferAtual = new ArrayList<>();
        String grupoIdAtual = null;

        for (AtividadeLog log : logs) {
            String grupoId = log.getGrupoId();
            boolean mesmoGrupoDoBuffer = grupoId != null && grupoId.equals(grupoIdAtual) && !bufferAtual.isEmpty();

            if (mesmoGrupoDoBuffer) {
                bufferAtual.add(log);
            } else {
                if (!bufferAtual.isEmpty()) {
                    grupos.add(new TimelineGrupoDTO(grupoIdAtual, new ArrayList<>(bufferAtual)));
                }
                bufferAtual = new ArrayList<>();
                bufferAtual.add(log);
                grupoIdAtual = grupoId;
            }
        }
        if (!bufferAtual.isEmpty()) {
            grupos.add(new TimelineGrupoDTO(grupoIdAtual, new ArrayList<>(bufferAtual)));
        }
        return grupos;
    }

    // --- Admin: exclusão ---

    @Transactional
    public void deletarLog(Long logId) {
        if (!repository.existsById(logId)) {
            throw new RuntimeException("Log não encontrado: " + logId);
        }
        repository.deleteById(logId);
    }

    @Transactional
    public void deletarTodosLogsDoUsuario(Long userId) {
        // busca logs pelo userId usando query direta
        List<AtividadeLog> logs = repository.findByUserIdOrderByCriadoEmDesc(userId);
        repository.deleteAll(logs);
    }

    @Transactional
    public void deletarLogsByUser(User user) {
        repository.deleteByUser(user);
    }

    // --- Helpers ---

    private void salvar(AtividadeLog log) {
        salvar(log, null);
    }

    private void salvar(AtividadeLog log, String grupoId) {
        try {
            log.setGrupoId(grupoId);
            repository.save(log);
        } catch (Exception e) {
            // Log silencioso - nunca deixa a ação principal falhar por causa do log
            System.err.println("[AtividadeLog] Erro ao salvar log: " + e.getMessage());
        }
    }

    private String formatarProgressoSerie(Integer temporada, Integer episodio) {
        if (temporada == null && episodio == null) return "Sem progresso";
        int t = temporada != null ? temporada : 1;
        int e = episodio != null ? episodio : 0;
        return "T" + t + ":EP" + e;
    }

    private String emojiTipo(String tipo) {
        if (tipo == null) return "📌";
        return switch (tipo.toLowerCase()) {
            case "filme"  -> "🎬";
            case "série"  -> "📺";
            case "jogo"   -> "🎮";
            default       -> "📌";
        };
    }

    private String emojiStatus(String status) {
        if (status == null) return "🔄";
        return switch (status.toLowerCase()) {
            case "assistido", "zerado"    -> "✅";
            case "assistindo", "jogando"  -> "▶️";
            case "backlog"                -> "📅";
            case "dropado"                -> "❌";
            default                       -> "🔄";
        };
    }
}
