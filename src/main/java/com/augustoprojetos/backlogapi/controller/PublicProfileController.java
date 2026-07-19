package com.augustoprojetos.backlogapi.controller;

import com.augustoprojetos.backlogapi.entity.Item;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.entity.UserConquista;
import com.augustoprojetos.backlogapi.repository.ItemRepository;
import com.augustoprojetos.backlogapi.repository.UserRepository;
import com.augustoprojetos.backlogapi.service.ConquistaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Optional;

@Controller
public class PublicProfileController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ConquistaService conquistaService;

    @GetMapping("/u/{socialUsername}")
    public String showPublicProfile(@PathVariable String socialUsername, Model model) {

        Optional<User> userOpt = userRepository.findBySocialUsername(socialUsername);

        // 2. Trava de Segurança: Se não existir ou se a chave "Perfil Público" estiver desligada
        if (userOpt.isEmpty() || !userOpt.get().isPublic()) {
            return "redirect:/home?error=privateProfile";
        }

        User targetUser = userOpt.get();
        List<Item> itens = itemRepository.findByUserOrderByTituloAsc(targetUser);

        // Dados de conquistas para o perfil público
        List<UserConquista> conquistas = conquistaService.listarConquistasDoUsuario(targetUser);
        int xpTotal   = conquistaService.calcularXpTotal(targetUser);
        int nivel     = conquistaService.calcularNivel(xpTotal);
        int xpBase    = conquistaService.xpBaseDoNivel(nivel);
        int xpProx    = conquistaService.xpParaProximoNivel(nivel);
        int xpNivel   = xpTotal - xpBase;
        int progresso = (int) ((xpNivel / (double) xpProx) * 100);

        model.addAttribute("apelido",    "@" + targetUser.getSocialUsername());
        model.addAttribute("itens",      itens);
        model.addAttribute("conquistas", conquistas);
        model.addAttribute("xpTotal",    xpTotal);
        model.addAttribute("nivel",      nivel);
        model.addAttribute("progresso",  progresso);

        return "shared";
    }
}
