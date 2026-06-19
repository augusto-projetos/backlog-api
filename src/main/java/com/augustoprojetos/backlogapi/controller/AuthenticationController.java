package com.augustoprojetos.backlogapi.controller;

import com.augustoprojetos.backlogapi.dto.RegisterDTO;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.entity.EmailVerificationToken;
import com.augustoprojetos.backlogapi.repository.UserRepository;
import com.augustoprojetos.backlogapi.repository.EmailVerificationTokenRepository;
import com.augustoprojetos.backlogapi.service.UserService;
import com.augustoprojetos.backlogapi.service.EmailService;
import com.augustoprojetos.backlogapi.service.RateLimitService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
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

    @Autowired
    private RateLimitService rateLimitService;

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }

    @GetMapping("/reenviar-email")
    public String reenviarEmailForm() {
        return "reenviar-email";
    }

    // 2. Recebe os dados do formulário, salva inativo e dispara o e-mail
    @PostMapping("/auth/register")
    public String register(RegisterDTO data) {

        // Verifica o rate limit para evitar spam
        if (!rateLimitService.isPermitido(data.email())) {
            return "redirect:/register?error=spam"; 
        }

        // Verifica se o email já existe
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

    // 3. Endpoint para reenvio do e-mail de verificação
    @PostMapping("/auth/reenviar-email")
    public String reenviarEmail(@RequestParam("email") String email) {
        // Proteção contra spam no reenvio
        if (!rateLimitService.isPermitido(email)) {
            return "redirect:/reenviar-email?error=spam";
        }

        Object userObj = userRepository.findByEmail(email);

        // Se o usuário existir e ainda não estiver verificado
        if (userObj instanceof User user && !user.isEnabled()) {
            // Apaga o token antigo se existir
            Optional<EmailVerificationToken> tokenAntigo = emailVerificationTokenRepository.findByUser_Id(user.getId());
            tokenAntigo.ifPresent(emailVerificationTokenRepository::delete);

            // Gera um novo e envia
            EmailVerificationToken novoToken = new EmailVerificationToken(user);
            emailVerificationTokenRepository.save(novoToken);
            emailService.sendVerificationEmail(user.getEmail(), novoToken.getToken());
        }

        return "redirect:/reenviar-email?resendSuccess";
    }

    // 4. Endpoint que o utilizador vai clicar no e-mail
    @GetMapping("/auth/verify")
    public String verifyEmail(@RequestParam("token") String token, HttpServletRequest request, HttpServletResponse response) {
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
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);

        // Vai direto para o home já autenticado
        return "redirect:/home";
    }

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }
}