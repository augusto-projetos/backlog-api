package com.augustoprojetos.backlogapi.controller;

import com.augustoprojetos.backlogapi.dto.ConquistaDesbloqueadaDTO;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.service.RecomendacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/recomendacoes")
public class RecomendacaoController {

    @Autowired
    private RecomendacaoService recomendacaoService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> pedirSugestao(
            @RequestParam(defaultValue = "Filme") String tipo,
            @AuthenticationPrincipal User user) {

        RecomendacaoService.RecomendacaoResult resultado =
                recomendacaoService.obterRecomendacaoDaIA(user, tipo);

        Map<String, Object> response = new HashMap<>();
        response.put("recomendacao", resultado.recomendacao());

        ConquistaDesbloqueadaDTO conquista = resultado.conquistaDesbloqueada();
        if (conquista != null) {
            response.put("conquistaDesbloqueada", conquista);
        }

        return ResponseEntity.ok(response);
    }
}
