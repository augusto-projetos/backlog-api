package com.augustoprojetos.backlogapi.controller;

import com.augustoprojetos.backlogapi.entity.Item;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.repository.ItemRepository;
import com.augustoprojetos.backlogapi.repository.UserRepository;
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

    // Captura qualquer coisa depois de /u/
    @GetMapping("/u/{socialUsername}")
    public String showPublicProfile(@PathVariable String socialUsername, Model model) {
        
        // 1. Busca o usuário pelo @
        Optional<User> userOpt = userRepository.findBySocialUsername(socialUsername);

        // 2. Trava de Segurança: Se não existir ou se a chave "Perfil Público" estiver desligada
        if (userOpt.isEmpty() || !userOpt.get().isPublic()) {
            // Redireciona de volta para a home se tentar acessar um perfil privado
            return "redirect:/home?error=privateProfile"; 
        }

        User targetUser = userOpt.get();

        // 3. Busca a coleção da pessoa
        List<Item> itens = itemRepository.findByUser(targetUser); 

        // 4. Injeta os dados no shared.html
        model.addAttribute("apelido", "@" + targetUser.getSocialUsername()); 
        model.addAttribute("itens", itens);

        // 5. Renderiza a tela
        return "shared";
    }
}