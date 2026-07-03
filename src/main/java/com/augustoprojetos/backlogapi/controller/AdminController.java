package com.augustoprojetos.backlogapi.controller;

import com.augustoprojetos.backlogapi.entity.Conquista;
import com.augustoprojetos.backlogapi.service.AdminService;
import com.augustoprojetos.backlogapi.service.AtividadeLogService;
import com.augustoprojetos.backlogapi.service.AuditLogService;
import com.augustoprojetos.backlogapi.service.EmailService;
import com.augustoprojetos.backlogapi.service.SystemConfigService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private AtividadeLogService atividadeLogService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private EmailService emailService;

    // --- PAINEL PRINCIPAL ---

    @GetMapping({"", "/"})
    public String painelAdmin(Model model) {
        model.addAttribute("stats",      adminService.calcularEstatisticasGlobais());
        model.addAttribute("usuarios",   adminService.listarUsuariosAtivos());
        model.addAttribute("pendentes",  adminService.listarUsuariosPendentes());
        model.addAttribute("conquistas", adminService.listarConquistas());
        // Badge da aba de auditoria
        model.addAttribute("totalAuditLogs", auditLogService.contarTotal());
        // Configurações do sistema (aba Sistema)
        model.addAttribute("systemConfigs", systemConfigService.mapaConfigs());
        return "admin/painel";
    }

    // --- API ESTATÍSTICAS ---

    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<?> getStats() {
        return ResponseEntity.ok(adminService.calcularEstatisticasGlobais());
    }

    // Lista todas as conquistas do sistema (para o dropdown de conceder)
    @GetMapping("/api/conquistas")
    @ResponseBody
    public ResponseEntity<?> getConquistas() {
        return ResponseEntity.ok(adminService.listarConquistas());
    }

    // Lista conquistas que um usuário específico já possui
    @GetMapping("/api/usuario/{id}/conquistas")
    @ResponseBody
    public ResponseEntity<?> getConquistasDoUsuario(@PathVariable Long id) {
        try {
            var lista = adminService.listarConquistasDoUsuario(id).stream().map(uc -> Map.of(
                "id",            uc.getConquista().getId(),
                "nome",          uc.getConquista().getNome(),
                "icone",         uc.getConquista().getIcone(),
                "xp",            uc.getConquista().getXp(),
                "desbloqueadaEm", uc.getDesbloqueadaEm() != null ? uc.getDesbloqueadaEm().toString() : ""
            )).toList();
            return ResponseEntity.ok(lista);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    // --- USUÁRIOS ---

    @GetMapping("/api/usuario/{id}/itens")
    @ResponseBody
    public ResponseEntity<?> getItensUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.listarItensPorUsuario(id));
    }

    @PostMapping("/usuario/{id}/editar")
    @ResponseBody
    public ResponseEntity<?> editarUsuario(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        try {
            String novoLogin  = body.get("login");
            String novoSocial = body.get("socialUsername");
            String novoEmail  = body.get("email");
            adminService.editarUsuario(id, novoLogin, novoSocial, novoEmail);
            String detalhe = "Login: " + novoLogin + " | @: " + novoSocial + " | Email: " + novoEmail;
            auditLogService.registrarUsuarioEditado(id, novoLogin, detalhe, getClientIp(request));
            return ResponseEntity.ok(Map.of("sucesso", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/usuario/{id}/senha")
    @ResponseBody
    public ResponseEntity<?> redefinirSenha(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        try {
            adminService.redefinirSenhaUsuario(id, body.get("novaSenha"));
            var usuario = adminService.buscarUsuarioPorId(id);
            auditLogService.registrarSenhaRedefinida(id, usuario.getLogin(), getClientIp(request));
            return ResponseEntity.ok(Map.of("sucesso", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @DeleteMapping("/usuario/{id}")
    @ResponseBody
    public ResponseEntity<?> deletarUsuario(
            @PathVariable Long id,
            HttpServletRequest request) {
        try {
            // Guarda os dados antes de deletar
            var usuario = adminService.buscarUsuarioPorId(id);
            String loginAlvo = usuario.getLogin();
            String emailAlvo = usuario.getEmail();
            adminService.deletarUsuario(id);
            auditLogService.registrarContaDeletada(emailAlvo, loginAlvo, getClientIp(request));
            return ResponseEntity.ok(Map.of("sucesso", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    // Concede uma conquista a um usuário
    @PostMapping("/usuario/{userId}/conceder-conquista/{conquistaId}")
    @ResponseBody
    public ResponseEntity<?> concederConquista(
            @PathVariable Long userId,
            @PathVariable Long conquistaId,
            HttpServletRequest request) {
        try {
            boolean concedida = adminService.concederConquistaParaUsuario(userId, conquistaId);
            if (concedida) {
                var usuario    = adminService.buscarUsuarioPorId(userId);
                var conquistas = adminService.listarConquistas();
                String nomeConquista = conquistas.stream()
                    .filter(c -> c.getId().equals(conquistaId))
                    .map(Conquista::getNome)
                    .findFirst().orElse("(desconhecida)");
                auditLogService.registrarConquistaConcedida(userId, usuario.getLogin(), nomeConquista, getClientIp(request));
                return ResponseEntity.ok(Map.of("sucesso", true, "mensagem", "Conquista concedida com sucesso!"));
            } else {
                return ResponseEntity.ok(Map.of("sucesso", false, "mensagem", "Usuário já possui essa conquista."));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    // Revoga uma conquista de um usuário (deduz o XP correspondente)
    @DeleteMapping("/usuario/{userId}/revogar-conquista/{conquistaId}")
    @ResponseBody
    public ResponseEntity<?> revogarConquista(
            @PathVariable Long userId,
            @PathVariable Long conquistaId,
            HttpServletRequest request) {
        try {
            // Busca o ícone antes de revogar para incluir na resposta
            String icone = adminService.listarConquistas().stream()
                    .filter(c -> c.getId().equals(conquistaId))
                    .map(c -> c.getIcone())
                    .findFirst().orElse("");
            String nomeConquista = adminService.listarConquistas().stream()
                    .filter(c -> c.getId().equals(conquistaId))
                    .map(Conquista::getNome)
                    .findFirst().orElse("(desconhecida)");
            int xpDeduzido = adminService.revogarConquistaDoUsuario(userId, conquistaId);
            if (xpDeduzido >= 0) {
                var usuario = adminService.buscarUsuarioPorId(userId);
                auditLogService.registrarConquistaRevogada(userId, usuario.getLogin(), nomeConquista, xpDeduzido, getClientIp(request));
                return ResponseEntity.ok(Map.of(
                    "sucesso",    true,
                    "xpDeduzido", xpDeduzido,
                    "icone",      icone,
                    "mensagem",   "Conquista revogada. -" + xpDeduzido + " XP descontados."
                ));
            } else {
                return ResponseEntity.ok(Map.of("sucesso", false, "mensagem", "Usuário não possui essa conquista."));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    // --- ITENS ---

    @PostMapping("/item/{id}/editar")
    @ResponseBody
    public ResponseEntity<?> editarItem(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            String status  = (String) body.get("status");
            Double nota    = body.get("nota") != null ? ((Number) body.get("nota")).doubleValue() : null;
            String resenha = (String) body.get("resenha");
            adminService.editarItem(id, status, nota, resenha);
            return ResponseEntity.ok(Map.of("sucesso", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @DeleteMapping("/item/{id}")
    @ResponseBody
    public ResponseEntity<?> deletarItem(
            @PathVariable Long id,
            HttpServletRequest request) {
        try {
            // Guarda dados antes de deletar
            var item = adminService.buscarItemPorId(id);
            adminService.deletarItem(id);
            auditLogService.registrarItemDeletado(id, item.getTitulo(), item.getUserLogin(), getClientIp(request));
            return ResponseEntity.ok(Map.of("sucesso", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    // --- CONQUISTAS ---

    @PostMapping("/conquista/criar")
    @ResponseBody
    public ResponseEntity<?> criarConquista(
            @RequestBody Conquista conquista,
            HttpServletRequest request) {
        try {
            adminService.criarConquista(conquista);
            // Busca o ID gerado
            var salva = adminService.listarConquistas().stream()
                .filter(c -> c.getChave().equals(conquista.getChave()))
                .findFirst().orElse(conquista);
            auditLogService.registrarConquistaCriada(salva.getId(), salva.getNome(), getClientIp(request));
            return ResponseEntity.ok(Map.of("sucesso", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/conquista/{id}/editar")
    @ResponseBody
    public ResponseEntity<?> editarConquista(
            @PathVariable Long id,
            @RequestBody Conquista dados,
            HttpServletRequest request) {
        try {
            adminService.editarConquista(id, dados);
            auditLogService.registrarConquistaEditada(id, dados.getNome(), getClientIp(request));
            return ResponseEntity.ok(Map.of("sucesso", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @DeleteMapping("/conquista/{id}")
    @ResponseBody
    public ResponseEntity<?> deletarConquista(
            @PathVariable Long id,
            HttpServletRequest request) {
        try {
            var conquista = adminService.buscarConquistaPorId(id);
            adminService.deletarConquista(id);
            auditLogService.registrarConquistaDeletada(id, conquista.getNome(), getClientIp(request));
            return ResponseEntity.ok(Map.of("sucesso", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    // --- TIMELINE (ADMIN) ---

    // Página HTML da timeline de um usuário específico
    @GetMapping("/usuario/{id}/timeline")
    public String timelineUsuario(@PathVariable Long id, Model model) {
        var usuario = adminService.buscarUsuarioPorId(id);
        var logs    = atividadeLogService.buscarTimelinePorUserId(id);
        model.addAttribute("usuarioAdm", usuario);
        model.addAttribute("logs", logs);
        return "admin/timeline-usuario";
    }

    // API: lista logs de um usuário (JSON)
    @GetMapping("/api/usuario/{id}/logs")
    @ResponseBody
    public ResponseEntity<?> getLogsUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(atividadeLogService.buscarTimelinePorUserId(id));
    }

    // Deleta um log específico
    @DeleteMapping("/log/{logId}")
    @ResponseBody
    public ResponseEntity<?> deletarLog(@PathVariable Long logId) {
        try {
            atividadeLogService.deletarLog(logId);
            return ResponseEntity.ok(Map.of("sucesso", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    // Deleta TODOS os logs de um usuário
    @DeleteMapping("/usuario/{id}/logs")
    @ResponseBody
    public ResponseEntity<?> deletarTodosLogsUsuario(@PathVariable Long id) {
        try {
            atividadeLogService.deletarTodosLogsDoUsuario(id);
            return ResponseEntity.ok(Map.of("sucesso", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    // --- AUDIT LOG (ADMIN) ---

    // API principal do log de auditoria com filtros e paginação.
    @GetMapping("/api/audit-log")
    @ResponseBody
    public ResponseEntity<?> getAuditLog(
            @RequestParam(required = false) String acao,
            @RequestParam(required = false) String alvoNome,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {

        Pageable pageable = PageRequest.of(page, size);
        var resultado = auditLogService.listarComFiltros(acao, alvoNome, pageable);

        // Serializa manualmente para evitar lazy loading issues
        var registros = resultado.getContent().stream().map(a -> {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id",         a.getId());
            m.put("acao",       a.getAcao());
            m.put("descricao",  a.getDescricao());
            m.put("detalhe",    a.getDetalhe());
            m.put("alvoTipo",   a.getAlvoTipo());
            m.put("alvoId",     a.getAlvoId());
            m.put("alvoNome",   a.getAlvoNome());
            m.put("criadoEm",   a.getCriadoEm() != null ? a.getCriadoEm().toString() : null);
            return m;
        }).toList();

        return ResponseEntity.ok(Map.of(
            "registros",    registros,
            "totalPaginas", resultado.getTotalPages(),
            "totalItens",   resultado.getTotalElements(),
            "paginaAtual",  resultado.getNumber()
        ));
    }

    // Lista as ações distintas para popular o dropdown de filtro
    @GetMapping("/api/audit-log/acoes")
    @ResponseBody
    public ResponseEntity<?> getAcoesAuditoria() {
        return ResponseEntity.ok(auditLogService.listarAcoesDistintas());
    }

    // --- SISTEMA ---

    @GetMapping("/api/sistema/configs")
    @ResponseBody
    public ResponseEntity<?> getSistemaConfigs() {
        return ResponseEntity.ok(systemConfigService.mapaConfigs());
    }

    // Retorna a lista de usuários ativos (id, login, email) para o seletor de destinatários do comunicado
    @GetMapping("/api/usuarios-para-email")
    @ResponseBody
    public ResponseEntity<?> getUsuariosParaEmail() {
        List<Map<String, Object>> lista = adminService.listarUsuariosAtivos().stream()
                .map(u -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id",    u.getId());
                    m.put("login", u.getLogin());
                    m.put("email", u.getEmail());
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    @PostMapping("/sistema/toggle/{chave}")
    @ResponseBody
    public ResponseEntity<?> toggleSistema(
            @PathVariable String chave,
            HttpServletRequest request) {
        try {
            systemConfigService.toggle(chave);
            boolean novoValor = systemConfigService.isAtivo(chave);
            String descAcao = novoValor ? "SISTEMA_ATIVADO" : "SISTEMA_DESATIVADO";
            auditLogService.registrarAcaoSistema(descAcao, chave, getClientIp(request));
            return ResponseEntity.ok(Map.of("sucesso", true, "ativo", novoValor));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/sistema/novidades")
    @ResponseBody
    public ResponseEntity<?> atualizarNovidades(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        try {
            String texto = body.getOrDefault("texto", "").strip();
            systemConfigService.setValor(SystemConfigService.NOVIDADES, texto);
            auditLogService.registrarAcaoSistema("NOVIDADES_ATUALIZADAS", texto.isBlank() ? "(removido)" : texto, getClientIp(request));
            return ResponseEntity.ok(Map.of("sucesso", true, "ativo", !texto.isBlank()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    // Dispara o comunicado por e-mail para os destinatários selecionados pelo admin
    @PostMapping("/sistema/novidades/email")
    @ResponseBody
    public ResponseEntity<?> enviarNovidadesPorEmail(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            String texto = ((String) body.getOrDefault("texto", "")).strip();
            if (texto.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("erro", "Escreva uma mensagem no campo de comunicado antes de disparar o e-mail."));
            }

            @SuppressWarnings("unchecked")
            List<String> destinatarios = (List<String>) body.get("destinatarios");
            if (destinatarios == null || destinatarios.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("erro", "Nenhum destinatário selecionado."));
            }

            // Garante que apenas e-mails de usuários ativos sejam aceitos (evita injeção de e-mails externos)
            List<String> ativos = adminService.listarEmailsUsuariosAtivos();
            List<String> destinatariosValidados = destinatarios.stream()
                    .filter(ativos::contains)
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());

            if (destinatariosValidados.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("erro", "Nenhum dos e-mails selecionados corresponde a um usuário ativo."));
            }

            emailService.sendBulkAnnouncementEmail(destinatariosValidados, texto);
            auditLogService.registrarAcaoSistema(
                    "NOVIDADES_EMAIL_DISPARADO",
                    texto + " (" + destinatariosValidados.size() + " destinatário" + (destinatariosValidados.size() == 1 ? "" : "s") + ")",
                    getClientIp(request)
            );

            return ResponseEntity.ok(Map.of("sucesso", true, "totalDestinatarios", destinatariosValidados.size()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    // --- HELPER: IP DO CLIENTE ---

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For pode conter múltiplos IPs separados por vírgula — pega o primeiro
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
