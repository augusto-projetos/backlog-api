package com.augustoprojetos.backlogapi.controller;

import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.entity.UserConquista;
import com.augustoprojetos.backlogapi.service.ConquistaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.augustoprojetos.backlogapi.repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/conquistas")
public class ConquistaController {

    @Autowired
    private ConquistaService conquistaService;

    @Autowired
    private UserRepository userRepository;

    // Retorna as conquistas, XP total e nível do usuário logado.
    @GetMapping("/meu-perfil")
    public ResponseEntity<Map<String, Object>> meuPerfil(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(buildPerfilConquistas(user));
    }

    // Retorna conquistas do perfil público (para shared.html).
    @GetMapping("/u/{socialUsername}")
    public ResponseEntity<Map<String, Object>> perfilPublico(@PathVariable String socialUsername) {
        Optional<User> userOpt = userRepository.findBySocialUsername(socialUsername);

        if (userOpt.isEmpty() || !userOpt.get().isPublic()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(buildPerfilConquistas(userOpt.get()));
    }

    private Map<String, Object> buildPerfilConquistas(User user) {
        List<UserConquista> conquistas = conquistaService.listarConquistasDoUsuario(user);
        int xpTotal = conquistaService.calcularXpTotal(user);
        int nivel   = conquistaService.calcularNivel(xpTotal);

        int xpBase          = conquistaService.xpBaseDoNivel(nivel);
        int xpParaProxNivel = conquistaService.xpParaProximoNivel(nivel);
        int xpNoNivelAtual  = xpTotal - xpBase;

        List<Map<String, Object>> lista = conquistas.stream().map(uc -> {
            Map<String, Object> m = new HashMap<>();
            m.put("chave",         uc.getConquista().getChave());
            m.put("nome",          uc.getConquista().getNome());
            m.put("descricao",     uc.getConquista().getDescricao());
            m.put("icone",         uc.getConquista().getIcone());
            m.put("xp",            uc.getConquista().getXp());
            m.put("desbloquedaEm", uc.getDesbloquedaEm().toString());
            return m;
        }).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("conquistas",       lista);
        result.put("xpTotal",          xpTotal);
        result.put("nivel",            nivel);
        result.put("xpNoNivelAtual",   xpNoNivelAtual);
        result.put("xpParaProxNivel",  xpParaProxNivel);
        result.put("progresso",        (int) ((xpNoNivelAtual / (double) xpParaProxNivel) * 100));

        return result;
    }
}