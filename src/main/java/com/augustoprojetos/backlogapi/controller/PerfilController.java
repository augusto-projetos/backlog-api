package com.augustoprojetos.backlogapi.controller;

import com.augustoprojetos.backlogapi.entity.Conquista;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.entity.UserConquista;
import com.augustoprojetos.backlogapi.service.ConquistaService;
import com.augustoprojetos.backlogapi.service.UserService;
import com.augustoprojetos.backlogapi.service.AtividadeLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/perfil")
public class PerfilController {

    @Autowired
    private UserService userService;

    @Autowired
    private ConquistaService conquistaService;

    @Autowired
    private AtividadeLogService atividadeLogService;

    // Mostrar a página - injeta dados de conquistas
    @GetMapping
    public String paginaPerfil(@AuthenticationPrincipal User user, Model model) {
        // Conquistas desbloqueadas pelo usuário
        List<UserConquista> conquistasDesbloqueadas = conquistaService.listarConquistasDoUsuario(user);
        Set<String> chavesDesbloqueadas = conquistasDesbloqueadas.stream()
                .map(uc -> uc.getConquista().getChave())
                .collect(Collectors.toSet());

        // Todas as conquistas para exibir as bloqueadas também
        List<Conquista> todasConquistas = conquistaService.listarTodasConquistas();

        // Lista ordenada: desbloqueadas primeiro, depois bloqueadas
        List<Map<String, Object>> conquistasView = new java.util.ArrayList<>();
        // 1. Desbloqueadas
        for (UserConquista uc : conquistasDesbloqueadas) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("conquista", uc.getConquista());
            item.put("desbloqueada", true);
            conquistasView.add(item);
        }
        // 2. Bloqueadas
        for (Conquista c : todasConquistas) {
            if (!chavesDesbloqueadas.contains(c.getChave())) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("conquista", c);
                item.put("desbloqueada", false);
                conquistasView.add(item);
            }
        }

        int xpTotal   = conquistaService.calcularXpTotal(user);
        int nivel     = conquistaService.calcularNivel(xpTotal);
        int xpBase    = conquistaService.xpBaseDoNivel(nivel);
        int xpProx    = conquistaService.xpParaProximoNivel(nivel);
        int xpNivel   = xpTotal - xpBase;
        int progresso = (int) ((xpNivel / (double) xpProx) * 100);

        // Emoji por faixa de nível (muda a cada 5 níveis)
        String nivelEmoji = calcularEmojiNivel(nivel);

        model.addAttribute("usuario",        user);
        model.addAttribute("conquistasView", conquistasView);
        model.addAttribute("xpTotal",        xpTotal);
        model.addAttribute("nivel",          nivel);
        model.addAttribute("progresso",      progresso);
        model.addAttribute("xpNivel",        xpNivel);
        model.addAttribute("xpProxNivel",    xpProx);
        model.addAttribute("nivelEmoji",     nivelEmoji);
        model.addAttribute("conquistas",     conquistasDesbloqueadas);
        return "perfil";
    }

    // API para Atualizar APELIDO
    @PutMapping("/apelido")
    public ResponseEntity<?> atualizarApelido(@AuthenticationPrincipal User user,
                                               @RequestBody Map<String, String> dados) {
        String novoApelido = dados.get("novoApelido");

        if (novoApelido == null || novoApelido.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("{\"message\": \"O apelido não pode ser vazio.\"}");
        }

        userService.atualizarApelido(user, novoApelido);

        // Registra na timeline
        atividadeLogService.registrarPerfilEditado(user);

        return ResponseEntity.ok().body("{\"message\": \"Apelido atualizado!\"}");
    }

    // API para Trocar Senha
    @PutMapping("/senha")
    public ResponseEntity<?> atualizarSenha(@AuthenticationPrincipal User user, @RequestBody Map<String, String> dados) {
        try {
            // O UserService lança exceções com a mensagem exata do erro
            userService.trocarSenha(user, dados.get("senhaAntiga"), dados.get("novaSenha"));
            return ResponseEntity.ok().body("{\"message\": \"Senha alterada com sucesso!\"}");

        } catch (IllegalArgumentException e) {
            // Retorna o erro específico (Senha incorreta OU Senha igual)
            return ResponseEntity.badRequest().body("{\"message\": \"" + e.getMessage() + "\"}");
        }
    }
    
    // API para Deletar Conta
    @DeleteMapping
    public ResponseEntity<?> deletarConta(@AuthenticationPrincipal User user,
                                          HttpServletRequest request, HttpServletResponse response) {
        userService.deletarConta(user);

        // Mata a sessão agora mesmo
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }

        return ResponseEntity.ok().build();
    }

    private String calcularEmojiNivel(int nivel) {
        if (nivel >= 50) return "🌌";
        if (nivel >= 45) return "🐉";
        if (nivel >= 40) return "👑";
        if (nivel >= 35) return "🦾";
        if (nivel >= 30) return "🌟";
        if (nivel >= 25) return "⚡";
        if (nivel >= 20) return "🏆";
        if (nivel >= 15) return "💎";
        if (nivel >= 10) return "🔥";
        if (nivel >= 5)  return "⚔️";
        return "🌱";
    }
}