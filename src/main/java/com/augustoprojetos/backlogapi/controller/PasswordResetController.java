package com.augustoprojetos.backlogapi.controller;

import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.repository.UserRepository;
import com.augustoprojetos.backlogapi.service.AuditLogService;
import com.augustoprojetos.backlogapi.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class PasswordResetController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditLogService auditLogService;

    // Recebe o e-mail da tela "Esqueci minha senha"
    @PostMapping("/recuperar-senha")
    public String processForgotPassword(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {

        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            String token = passwordResetService.createToken(user);
            passwordResetService.sendResetEmail(user.getEmail(), token);
        }

        return "redirect:/login?emailSent=true";
    }

    // Recebe a nova senha e o token da tela "Resetar Senha"
    @PostMapping("/resetar-senha")
    public String processResetPassword(@RequestParam("token") String token,
                                       @RequestParam("password") String password,
                                       RedirectAttributes redirectAttributes) {

        String result = passwordResetService.validatePasswordResetToken(token);

        if (!result.equals("valid")) {
            return "redirect:/login?resetError=true";
        }

        // Verifica se a senha atende aos requisitos antes de salvar
        if (!isValidPassword(password)) {
            redirectAttributes.addFlashAttribute("error", "A senha não atende aos requisitos mínimos de segurança.");
            return "redirect:/resetar-senha?token=" + token;
        }

        Optional<User> userOptional = passwordResetService.getUserByPasswordResetToken(token);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            // Criptografa a nova senha antes de salvar
            user.setPassword(passwordEncoder.encode(password));
            userRepository.save(user);

            // Registra no audit log
            auditLogService.registrarSenhaAlteradaPeloUsuario(user.getEmail(), "desconhecido");

            // Apaga o token usado
            passwordResetService.deleteToken(token);

            return "redirect:/login?resetSuccess=true";
        }

        return "redirect:/login";
    }

    // Mostra a tela de pedir o e-mail
    @GetMapping("/recuperar-senha")
    public String showForgotPasswordForm() {
        return "recuperar-senha";
    }

    // Mostra a tela de digitar a nova senha (só abre se tiver um token na URL)
    @GetMapping("/resetar-senha")
    public String showResetPasswordForm(@RequestParam("token") String token, Model model, RedirectAttributes redirectAttributes) {
        // Valida se o token existe antes de mostrar a tela
        String result = passwordResetService.validatePasswordResetToken(token);
        if (!result.equals("valid")) {
            return "redirect:/login?resetError=true";
        }

        // Passa o token para o HTML (ele vai ficar escondido no formulário)
        model.addAttribute("token", token);
        return "resetar-senha";
    }

    // --- METODO AUXILIAR DE VALIDAÇÃO ---
    // Valida: mín 8 chars, 1 maiúscula, 1 minúscula, 1 número e 1 caractere especial
    private boolean isValidPassword(String password) {
        if (password == null) {
            return false;
        }
        // Expressão Regular (Regex) para validar a força da senha
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._-])[A-Za-z\\d@$!%*?&._-]{8,}$";
        return password.matches(regex);
    }
}
