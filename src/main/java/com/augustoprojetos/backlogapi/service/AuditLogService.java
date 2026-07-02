package com.augustoprojetos.backlogapi.service;

import com.augustoprojetos.backlogapi.entity.AuditLog;
import com.augustoprojetos.backlogapi.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository repository;

    // --- REGISTRO DE EVENTOS CRÍTICOS ---

    // Conta de usuário criada com sucesso (verificação concluída)
    public void registrarContaCriada(String email, String login, String ip) {
        salvar(AuditLog.of(
            "CONTA_CRIADA",
            "Nova conta criada: @" + login + " (" + email + ")",
            "USUARIO", null, login,
            "Email: " + email, ip
        ));
    }

    // Admin deletou uma conta de usuário
    public void registrarContaDeletada(String emailAlvo, String loginAlvo, String ip) {
        salvar(AuditLog.of(
            "CONTA_DELETADA",
            "Conta deletada pelo administrador: @" + loginAlvo + " (" + emailAlvo + ")",
            "USUARIO", null, loginAlvo,
            "Email removido: " + emailAlvo, ip
        ));
    }

    // Admin redefiniu senha de um usuário
    public void registrarSenhaRedefinida(Long userId, String loginAlvo, String ip) {
        salvar(AuditLog.of(
            "SENHA_REDEFINIDA",
            "Senha redefinida pelo administrador para: @" + loginAlvo,
            "USUARIO", userId, loginAlvo,
            null, ip
        ));
    }

    // Admin editou dados de um usuário (login, email, @)
    public void registrarUsuarioEditado(Long userId, String loginAlvo, String detalhe, String ip) {
        salvar(AuditLog.of(
            "USUARIO_EDITADO",
            "Dados do usuário editados pelo administrador: @" + loginAlvo,
            "USUARIO", userId, loginAlvo,
            detalhe, ip
        ));
    }

    // Admin criou uma nova conquista
    public void registrarConquistaCriada(Long conquistaId, String nomeConquista, String ip) {
        salvar(AuditLog.of(
            "CONQUISTA_CRIADA",
            "Nova conquista criada: \"" + nomeConquista + "\"",
            "CONQUISTA", conquistaId, nomeConquista,
            null, ip
        ));
    }

    // Admin editou uma conquista existente
    public void registrarConquistaEditada(Long conquistaId, String nomeConquista, String ip) {
        salvar(AuditLog.of(
            "CONQUISTA_EDITADA",
            "Conquista editada: \"" + nomeConquista + "\"",
            "CONQUISTA", conquistaId, nomeConquista,
            null, ip
        ));
    }

    // Admin deletou uma conquista
    public void registrarConquistaDeletada(Long conquistaId, String nomeConquista, String ip) {
        salvar(AuditLog.of(
            "CONQUISTA_DELETADA",
            "Conquista deletada: \"" + nomeConquista + "\"",
            "CONQUISTA", conquistaId, nomeConquista,
            null, ip
        ));
    }

    // Admin concedeu conquista manualmente a um usuário
    public void registrarConquistaConcedida(Long userId, String loginAlvo, String nomeConquista, String ip) {
        salvar(AuditLog.of(
            "CONQUISTA_CONCEDIDA",
            "Conquista \"" + nomeConquista + "\" concedida manualmente a @" + loginAlvo,
            "USUARIO", userId, loginAlvo,
            "Conquista: " + nomeConquista, ip
        ));
    }

    // Admin revogou conquista de um usuário
    public void registrarConquistaRevogada(Long userId, String loginAlvo, String nomeConquista, int xpDeduzido, String ip) {
        salvar(AuditLog.of(
            "CONQUISTA_REVOGADA",
            "Conquista \"" + nomeConquista + "\" revogada de @" + loginAlvo,
            "USUARIO", userId, loginAlvo,
            "Conquista: " + nomeConquista + " | XP deduzido: " + xpDeduzido, ip
        ));
    }

    // Admin deletou um item do backlog de um usuário
    public void registrarItemDeletado(Long itemId, String tituloItem, String loginDono, String ip) {
        salvar(AuditLog.of(
            "ITEM_DELETADO",
            "Item \"" + tituloItem + "\" deletado pelo administrador (dono: @" + loginDono + ")",
            "ITEM", itemId, tituloItem,
            "Dono: @" + loginDono, ip
        ));
    }

    // Usuário alterou própria senha (via reset ou perfil)
    public void registrarSenhaAlteradaPeloUsuario(String email, String ip) {
        salvar(AuditLog.of(
            "SENHA_ALTERADA",
            "Senha alterada pelo próprio usuário: " + email,
            "USUARIO", null, email,
            null, ip
        ));
    }

    // --- LEITURA (ADMIN) ---

    public Page<AuditLog> listarTodos(Pageable pageable) {
        return repository.findAllByOrderByCriadoEmDesc(pageable);
    }

    public Page<AuditLog> listarComFiltros(String acao, String alvoNome, Pageable pageable) {
        boolean temAcao   = acao     != null && !acao.isBlank();
        boolean temAlvo   = alvoNome != null && !alvoNome.isBlank();

        if (!temAcao && !temAlvo) {
            return repository.findAllByOrderByCriadoEmDesc(pageable);
        }
        return repository.findWithFilters(
            temAcao ? acao     : null,
            temAlvo ? alvoNome : null,
            pageable
        );
    }

    public List<String> listarAcoesDistintas() {
        return repository.findAcoesDistintas();
    }

    public long contarTotal() {
        return repository.count();
    }

    // --- LIMPEZA AUTOMÁTICA (chamada pelo CleanupTask) ---

    @Transactional
    public int limparLogsAntigos(int diasRetencao) {
        LocalDateTime limite = LocalDateTime.now().minusDays(diasRetencao);
        int removidos = repository.deleteBycriadoEmBefore(limite);
        if (removidos > 0) {
            System.out.println("[AuditLog] 🧹 Limpeza automática: " + removidos
                + " registros de auditoria removidos (mais antigos que " + diasRetencao + " dias).");
        }
        return removidos;
    }

    // --- INTERNAL ---

    public void registrarAcaoSistema(String acao, String detalhe, String ip) {
        AuditLog log = new AuditLog();
        log.setAcao(acao);
        log.setDescricao("Configuração do sistema alterada: " + detalhe);
        log.setDetalhe(detalhe);
        log.setAlvoTipo("SISTEMA");
        log.setIp(ip);
        salvar(log);
    }

    private void salvar(AuditLog log) {
        try {
            repository.save(log);
        } catch (Exception e) {
            System.err.println("[AuditLog] Erro ao salvar registro: " + e.getMessage());
        }
    }
}
