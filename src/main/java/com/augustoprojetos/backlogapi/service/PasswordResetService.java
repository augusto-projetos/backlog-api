package com.augustoprojetos.backlogapi.service;

import com.augustoprojetos.backlogapi.entity.PasswordResetToken;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.repository.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private JavaMailSender mailSender;

    // Gera um token válido por 15 minutos
    public String createToken(User user) {
        // 1. Busca se o usuário já tem um token perdido lá no banco e apaga
        Optional<PasswordResetToken> existingToken = tokenRepository.findByUser(user);
        existingToken.ifPresent(tokenRepository::delete);

        // 2. Cria um novo
        String token = UUID.randomUUID().toString();
        PasswordResetToken myToken = new PasswordResetToken(
                token,
                user,
                LocalDateTime.now().plusMinutes(15)
        );
        tokenRepository.save(myToken);
        return token;
    }

    // Valida se o token existe e não está expirado
    public String validatePasswordResetToken(String token) {
        Optional<PasswordResetToken> passToken = tokenRepository.findByToken(token);

        if (passToken.isEmpty()) {
            return "invalidToken";
        }

        PasswordResetToken resetToken = passToken.get();
        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken); // Limpa o banco se já expirou
            return "expired";
        }

        return "valid";
    }

    // Envia o e-mail de fato
    public void sendResetEmail(String userEmail, String token) {
        // Altere a URL abaixo para a URL do seu Render quando for subir para produção
        String resetUrl = "http://meus-backlog.onrender.com/resetar-senha?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(userEmail);
        message.setSubject("Recuperação de Senha - Meus Backlog");
        message.setText("Você solicitou a redefinição de sua senha.\n\n" +
                "Clique no link abaixo para criar uma nova senha:\n" + resetUrl +
                "\n\nEste link expira em 15 minutos.");

        mailSender.send(message);
    }

    // Busca o usuário dono do token para podermos trocar a senha dele
    public Optional<User> getUserByPasswordResetToken(String token) {
        return tokenRepository.findByToken(token).map(PasswordResetToken::getUser);
    }

    // Deleta o token após o uso para não ser usado duas vezes
    public void deleteToken(String token) {
        tokenRepository.findByToken(token).ifPresent(tokenRepository::delete);
    }
}