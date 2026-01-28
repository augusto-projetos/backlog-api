package com.augustoprojetos.backlogapi.controller;

import com.augustoprojetos.backlogapi.dto.RegisterDTO;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthenticationController {

    @Autowired
    private UserRepository userRepository;

    // 1. Abre a tela de registro
    @GetMapping("/register")
    public String registerForm() {
        return "register"; // Vai procurar o register.html
    }

    // 2. Recebe os dados do formulário e salva no banco
    @PostMapping("/auth/register")
    public String register(RegisterDTO data) {
        // Verifica se já existe esse login
        if (userRepository.findByLogin(data.login()) != null) {
            return "redirect:/register?error"; // Volta com erro se já existir
        }

        // CRIPTOGRAFA A SENHA
        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());

        // Cria o usuário novo
        User newUser = new User();
        newUser.setLogin(data.login());
        newUser.setPassword(encryptedPassword);
        newUser.setEmail(data.email());

        // Salva no banco
        userRepository.save(newUser);

        // Manda pro login para ele entrar
        return "redirect:/login?success";
    }

    // 3. Abre a tela de login
    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }
}