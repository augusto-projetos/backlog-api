package com.augustoprojetos.backlogapi.service;

import com.augustoprojetos.backlogapi.entity.AtividadeLog;
import com.augustoprojetos.backlogapi.entity.Item;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.repository.AtividadeLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AtividadeLogService {

    @Autowired
    private AtividadeLogRepository repository;

    // --- Métodos de registro ---

    public void registrarContaCriada(User user) {
        salvar(AtividadeLog.of(user,
                "CONTA_CRIADA",
                "Criou a conta no Meu Backlog 🎉"));
    }

    public void registrarItemAdicionado(User user, Item item) {
        String tipo = item.getTipo();
        String emoji = emojiTipo(tipo);
        salvar(AtividadeLog.of(user,
                "ITEM_ADICIONADO",
                emoji + " Adicionou \"" + item.getTitulo() + "\" ao backlog",
                "Tipo: " + tipo + " • Status inicial: " + item.getStatus(),
                item.getTitulo()));
    }

    public void registrarItemRemovido(User user, Item item) {
        String emoji = emojiTipo(item.getTipo());
        salvar(AtividadeLog.of(user,
                "ITEM_REMOVIDO",
                emoji + " Removeu \"" + item.getTitulo() + "\" do backlog",
                "Tipo: " + item.getTipo(),
                item.getTitulo()));
    }

    public void registrarStatusAlterado(User user, Item item, String statusAnterior) {
        String emoji = emojiStatus(item.getStatus());
        String detalhe = statusAnterior + " → " + item.getStatus();
        salvar(AtividadeLog.of(user,
                "STATUS_ALTERADO",
                emoji + " Mudou o status de \"" + item.getTitulo() + "\"",
                detalhe,
                item.getTitulo()));
    }

    public void registrarNotaAlterada(User user, Item item, Double notaAnterior) {
        String detalhe = notaAnterior != null
                ? notaAnterior + " → " + item.getNota()
                : "Nota definida: " + item.getNota();
        salvar(AtividadeLog.of(user,
                "NOTA_ALTERADA",
                "⭐ Alterou a nota de \"" + item.getTitulo() + "\"",
                detalhe,
                item.getTitulo()));
    }

    public void registrarResenhaAdicionada(User user, Item item) {
        salvar(AtividadeLog.of(user,
                "RESENHA_ADICIONADA",
                "✍️ Escreveu uma resenha para \"" + item.getTitulo() + "\"",
                null,
                item.getTitulo()));
    }

    public void registrarResenhaEditada(User user, Item item) {
        salvar(AtividadeLog.of(user,
                "RESENHA_EDITADA",
                "📝 Editou a resenha de \"" + item.getTitulo() + "\"",
                null,
                item.getTitulo()));
    }

    public void registrarItemEditado(User user, Item item) {
        String emoji = emojiTipo(item.getTipo());
        salvar(AtividadeLog.of(user,
                "ITEM_EDITADO",
                emoji + " Editou \"" + item.getTitulo() + "\"",
                null,
                item.getTitulo()));
    }

    public void registrarConquistaDesbloqueada(User user, String nomeConquista, String icone) {
        salvar(AtividadeLog.of(user,
                "CONQUISTA_DESBLOQUEADA",
                (icone != null ? icone + " " : "🏆 ") + "Desbloqueou a conquista \"" + nomeConquista + "\"",
                null,
                null));
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
        try {
            repository.save(log);
        } catch (Exception e) {
            // Log silencioso — nunca deixa a ação principal falhar por causa do log
            System.err.println("[AtividadeLog] Erro ao salvar log: " + e.getMessage());
        }
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
