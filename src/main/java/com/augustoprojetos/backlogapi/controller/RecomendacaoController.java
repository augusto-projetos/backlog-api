package com.augustoprojetos.backlogapi.controller;

import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.service.RecomendacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/recomendacoes")
public class RecomendacaoController {

    @Autowired
    private RecomendacaoService recomendacaoService;

    @GetMapping
    public ResponseEntity<Map<String, String>> pedirSugestao(
            @RequestParam(defaultValue = "Filme") String tipo,
            @AuthenticationPrincipal User user) { // Resgata o usuário logado na sessão do Spring Security
        
        // Pede o serviço do Gemini
        String respostaIA = recomendacaoService.obterRecomendacaoDaIA(user, tipo);
        
        // Retorna um JSON amigável para o JavaScript ler
        return ResponseEntity.ok(Map.of("recomendacao", respostaIA));
    }
}