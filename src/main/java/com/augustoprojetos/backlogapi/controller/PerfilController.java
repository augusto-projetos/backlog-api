package com.augustoprojetos.backlogapi.controller;

import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/perfil")
public class PerfilController {

    @Autowired
    private UserService userService;

    // Mostrar a página
    @GetMapping
    public String paginaPerfil(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("usuario", user);
        return "perfil";
    }

    // API para Atualizar APELIDO
    @PutMapping("/apelido")
    public ResponseEntity<?> atualizarApelido(@AuthenticationPrincipal User user, @RequestBody Map<String, String> dados) {
        String novoApelido = dados.get("novoApelido");

        if (novoApelido == null || novoApelido.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("{\"message\": \"O apelido não pode ser vazio.\"}");
        }

        userService.atualizarApelido(user, novoApelido);
        return ResponseEntity.ok().body("{\"message\": \"Apelido atualizado!\"}");
    }

    // API para Trocar Senha
    @PutMapping("/senha")
    public ResponseEntity<?> atualizarSenha(@AuthenticationPrincipal User user, @RequestBody Map<String, String> dados) {
        try {
            // Agora o UserService lança exceções com a mensagem exata do erro
            userService.trocarSenha(user, dados.get("senhaAntiga"), dados.get("novaSenha"));
            return ResponseEntity.ok().body("{\"message\": \"Senha alterada com sucesso!\"}");

        } catch (IllegalArgumentException e) {
            // Retorna o erro específico (Senha incorreta OU Senha igual)
            return ResponseEntity.badRequest().body("{\"message\": \"" + e.getMessage() + "\"}");
        }
    }

    // API para Deletar Conta
    @DeleteMapping
    public ResponseEntity<?> deletarConta(@AuthenticationPrincipal User user) {
        userService.deletarConta(user);
        return ResponseEntity.ok().build();
    }
}