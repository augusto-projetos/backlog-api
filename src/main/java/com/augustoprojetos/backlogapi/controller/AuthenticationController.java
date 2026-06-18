package com.augustoprojetos.backlogapi.controller;

import com.augustoprojetos.backlogapi.dto.RegisterDTO;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.entity.EmailVerificationToken;
import com.augustoprojetos.backlogapi.repository.UserRepository;
import com.augustoprojetos.backlogapi.repository.EmailVerificationTokenRepository;
import com.augustoprojetos.backlogapi.service.UserService;
import com.augustoprojetos.backlogapi.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class AuthenticationController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private EmailService emailService;

    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }

    // 2. Recebe os dados do formulário, salva inativo e dispara o e-mail
    @PostMapping("/auth/register")
    public String register(RegisterDTO data) {
        // 1. Verifica se o EMAIL já existe
        if (userRepository.findByEmail(data.email()) != null) {
            return "redirect:/register?error=email";
        }

        try {
            User newUser = new User();
            newUser.setLogin(data.login());
            newUser.setEmail(data.email());
            newUser.setPassword(data.password());
            
            // Garante que inicia como não verificado (bloqueado no login normal)
            newUser.setEmailVerified(false); 

            userService.cadastrarUsuario(newUser);

            // Procura o utilizador acabado de guardar para garantir que temos o ID correto
            User savedUser = (User) userRepository.findByEmail(data.email());

            // Cria e guarda o token de verificação (expira em 24h automaticamente)
            EmailVerificationToken verificationToken = new EmailVerificationToken(savedUser);
            emailVerificationTokenRepository.save(verificationToken);

            // Envia o e-mail de forma assíncrona usando a API do Brevo
            emailService.sendVerificationEmail(savedUser.getEmail(), verificationToken.getToken());

            // Redireciona para o login com um aviso de que a verificação está pendente
            return "redirect:/login?pendingVerification";

        } catch (IllegalArgumentException e) {
            return "redirect:/register?error=senha";
        }
    }

    // 3. Endpoint que o utilizador vai clicar no e-mail
    @GetMapping("/auth/verify")
    public String verifyEmail(@RequestParam("token") String token) {
        Optional<EmailVerificationToken> optionalToken = emailVerificationTokenRepository.findByToken(token);

        if (optionalToken.isEmpty()) {
            return "redirect:/login?error=tokenInvalid";
        }

        EmailVerificationToken verificationToken = optionalToken.get();
        User user = verificationToken.getUser();

        // Se o link/token tiver expirado (passou de 24h), deleta o token e a conta do utilizador
        if (verificationToken.isExpired()) {
            emailVerificationTokenRepository.delete(verificationToken);
            userRepository.delete(user);
            return "redirect:/register?error=tokenExpired";
        }

        // Se estiver tudo OK, ativa a conta do utilizador
        user.setEmailVerified(true);
        userRepository.save(user);

        // Apaga o token para que não seja reutilizado
        emailVerificationTokenRepository.delete(verificationToken);

        // Faz o auto-login programático no Spring Security
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Vai direto para o home já autenticado
        return "redirect:/home";
    }

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }
}