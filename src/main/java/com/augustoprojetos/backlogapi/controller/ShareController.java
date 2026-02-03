package com.augustoprojetos.backlogapi.controller;

import com.augustoprojetos.backlogapi.entity.Item;
import com.augustoprojetos.backlogapi.entity.ShareToken;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.repository.ItemRepository;
import com.augustoprojetos.backlogapi.service.ShareTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class ShareController {

    @Autowired
    private ShareTokenService shareTokenService;

    @Autowired
    private ItemRepository itemRepository;

    // --- ROTA PÚBLICA ---
    // Ex: site.com/share/550e8400-e29b...
    @GetMapping("/share/{token}")
    public String visualizarCompartilhado(@PathVariable String token, Model model) {
        // 1. Verifica se o token existe e não venceu
        Optional<ShareToken> tokenOpt = shareTokenService.obterTokenValido(token);

        if (tokenOpt.isEmpty()) {
            return "error/404"; // Se venceu ou não existe, manda pra página de erro
        }

        ShareToken shareToken = tokenOpt.get();
        User donoDaLista = shareToken.getUser();

        // 2. Busca os itens do DONO do token (não de quem está acessando)
        List<Item> itens = itemRepository.findByUser(donoDaLista);

        // 3. Manda os dados para a nova página
        model.addAttribute("apelido", donoDaLista.getLogin());
        model.addAttribute("itens", itens);

        // Retorna o HTML específico de leitura (sem botões de editar)
        return "shared";
    }

    // --- ROTAS PRIVADAS (Painel de Controle do Usuário) ---

    // Listar meus links ativos
    @GetMapping("/api/share/meus-links")
    @ResponseBody
    public List<ShareToken> listarMeusLinks(@AuthenticationPrincipal User user) {
        return shareTokenService.listarMeusLinks(user);
    }

    // Gerar novo link
    @PostMapping("/api/share/gerar")
    @ResponseBody
    public ResponseEntity<?> gerarLink(@RequestBody Map<String, Integer> payload, @AuthenticationPrincipal User user) {
        int horas = payload.getOrDefault("horas", 24); // Padrão 24h se não enviar nada
        ShareToken token = shareTokenService.gerarToken(user, horas);
        return ResponseEntity.ok(token);
    }

    // Apagar link (Revogar acesso)
    @DeleteMapping("/api/share/{id}")
    @ResponseBody
    public ResponseEntity<?> revogarLink(@PathVariable Long id) {
        shareTokenService.revogarToken(id);
        return ResponseEntity.ok().build();
    }

    // Rota para abrir a página de gerenciamento
    @GetMapping("/meus-links")
    public String paginaMeusLinks() {
        return "links";
    }
}