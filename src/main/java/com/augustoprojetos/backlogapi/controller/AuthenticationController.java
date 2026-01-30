package com.augustoprojetos.backlogapi.controller;

import com.augustoprojetos.backlogapi.dto.RegisterDTO;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.repository.UserRepository;
import com.augustoprojetos.backlogapi.service.UserService; // Importante
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthenticationController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService; // Injetar o Service

    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }

    // 2. Recebe os dados do formulário e salva no banco
    @PostMapping("/auth/register")
    public String register(RegisterDTO data) {
        // 1. Verifica se o EMAIL já existe
        if (userRepository.findByEmail(data.email()) != null) {
            return "redirect:/register?error=email"; // Erro específico de email
        }

        try {
            User newUser = new User();
            newUser.setLogin(data.login());
            newUser.setEmail(data.email());
            // Passa a senha pura para o Service validar
            newUser.setPassword(data.password());

            userService.cadastrarUsuario(newUser);

            return "redirect:/login?success";

        } catch (IllegalArgumentException e) {
            // 2. Se a senha for fraca (ou outro erro de validação)
            return "redirect:/register?error=senha"; // Erro específico de senha
        }
    }

    // 3. Abre a tela de login
    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }
}