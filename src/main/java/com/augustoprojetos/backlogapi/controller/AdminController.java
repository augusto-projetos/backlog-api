package com.augustoprojetos.backlogapi.controller;

import com.augustoprojetos.backlogapi.entity.Conquista;
import com.augustoprojetos.backlogapi.service.AdminService;
import com.augustoprojetos.backlogapi.service.AtividadeLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private AtividadeLogService atividadeLogService;

    // --- PAINEL PRINCIPAL ---

    @GetMapping({"", "/"})
    public String painelAdmin(Model model) {
        model.addAttribute("stats",      adminService.calcularEstatisticasGlobais());
        model.addAttribute("usuarios",   adminService.listarUsuariosAtivos());
        model.addAttribute("pendentes",  adminService.listarUsuariosPendentes());
        model.addAttribute("conquistas", adminService.listarConquistas());
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
                "desbloquedaEm", uc.getDesbloquedaEm() != null ? uc.getDesbloquedaEm().toString() : ""
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
            @RequestBody Map<String, String> body) {
        try {
            adminService.editarUsuario(id, body.get("login"), body.get("socialUsername"), body.get("email"));
            return ResponseEntity.ok(Map.of("sucesso", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/usuario/{id}/senha")
    @ResponseBody
    public ResponseEntity<?> redefinirSenha(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            adminService.redefinirSenhaUsuario(id, body.get("novaSenha"));
            return ResponseEntity.ok(Map.of("sucesso", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @DeleteMapping("/usuario/{id}")
    @ResponseBody
    public ResponseEntity<?> deletarUsuario(@PathVariable Long id) {
        try {
            adminService.deletarUsuario(id);
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
            @PathVariable Long conquistaId) {
        try {
            boolean concedida = adminService.concederConquistaParaUsuario(userId, conquistaId);
            if (concedida) {
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
            @PathVariable Long conquistaId) {
        try {
            // Busca o ícone antes de revogar para incluir na resposta
            String icone = adminService.listarConquistas().stream()
                    .filter(c -> c.getId().equals(conquistaId))
                    .map(c -> c.getIcone())
                    .findFirst().orElse("");
            int xpDeduzido = adminService.revogarConquistaDoUsuario(userId, conquistaId);
            if (xpDeduzido >= 0) {
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
    public ResponseEntity<?> deletarItem(@PathVariable Long id) {
        try {
            adminService.deletarItem(id);
            return ResponseEntity.ok(Map.of("sucesso", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    // --- CONQUISTAS ---

    @PostMapping("/conquista/criar")
    @ResponseBody
    public ResponseEntity<?> criarConquista(@RequestBody Conquista conquista) {
        try {
            adminService.criarConquista(conquista);
            return ResponseEntity.ok(Map.of("sucesso", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/conquista/{id}/editar")
    @ResponseBody
    public ResponseEntity<?> editarConquista(@PathVariable Long id, @RequestBody Conquista dados) {
        try {
            adminService.editarConquista(id, dados);
            return ResponseEntity.ok(Map.of("sucesso", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @DeleteMapping("/conquista/{id}")
    @ResponseBody
    public ResponseEntity<?> deletarConquista(@PathVariable Long id) {
        try {
            adminService.deletarConquista(id);
            return ResponseEntity.ok(Map.of("sucesso", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    // --- TIMELINE (ADMIN) ---

    // Página HTML da timeline de um usuário específico
    @GetMapping("/usuario/{id}/timeline")
    public String timelineUsuario(@PathVariable Long id, Model model) {
        var usuario = adminService.buscarUsuarioPorId(id); // já existente
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
}