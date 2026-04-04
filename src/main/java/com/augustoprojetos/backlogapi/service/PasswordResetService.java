package com.augustoprojetos.backlogapi.service;

import com.augustoprojetos.backlogapi.entity.PasswordResetToken;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.repository.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;

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

    // Envia o e-mail formatado em HTML
    public void sendResetEmail(String userEmail, String token) {
        String resetUrl = "https://meus-backlog.onrender.com/resetar-senha?token=" + token;

        // Caminho da sua logo hospedada no seu próprio site
        String logoUrl = "https://meus-backlog.onrender.com/img/logo.png";

        try {
            // Cria uma mensagem do tipo MIME (que suporta HTML)
            jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();

            // O "true" indica que a mensagem será multipart (permite HTML)
            org.springframework.mail.javamail.MimeMessageHelper helper =
                    new org.springframework.mail.javamail.MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(userEmail);
            helper.setSubject("Recuperação de Senha - Meus Backlog");

            // Montando o HTML do E-mail
            String htmlMsg =
                    "<div style='font-family: Arial, sans-serif; background-color: #1a1a2e; color: #ffffff; padding: 40px 20px; text-align: center; border-radius: 10px; max-width: 600px; margin: 0 auto; border: 1px solid #333;'>"
                            + "  <img src='" + logoUrl + "' alt='Meus Backlog Logo' style='max-width: 250px; margin-bottom: 20px;'>"
                            + "  <h2 style='color: #ffffff; margin-bottom: 10px;'>Recuperação de Senha</h2>"
                            + "  <p style='font-size: 16px; color: #cccccc; line-height: 1.5;'>Você solicitou a redefinição de sua senha.<br>Clique no botão abaixo para criar uma nova senha:</p>"
                            + "  <a href='" + resetUrl + "' style='display: inline-block; padding: 14px 28px; margin: 25px 0; background-color: #e94560; color: #ffffff; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 16px;'>Redefinir Minha Senha</a>"
                            + "  <p style='font-size: 14px; color: #888888; margin-top: 30px;'>Este link expira com segurança em 15 minutos.</p>"
                            + "  <p style='font-size: 12px; color: #555555; margin-top: 10px;'>Se você não solicitou essa alteração, apenas ignore este e-mail.</p>"
                            + "</div>";

            // O segundo parâmetro 'true' informa ao Java que o texto deve ser lido como HTML
            helper.setText(htmlMsg, true);

            mailSender.send(message);

        } catch (jakarta.mail.MessagingException e) {
            e.printStackTrace();
            System.out.println("Erro ao tentar enviar o e-mail HTML: " + e.getMessage());
        }
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