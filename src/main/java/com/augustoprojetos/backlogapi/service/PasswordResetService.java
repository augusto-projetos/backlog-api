package com.augustoprojetos.backlogapi.service;

import com.augustoprojetos.backlogapi.entity.PasswordResetToken;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.repository.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Value("${BREVO_API_KEY}")
    private String brevoApiKey;

    // Gera um token válido por 15 minutos
    public String createToken(User user) {
        // 1. Busca se o usuário já tem um token perdido lá no banco e apaga
        Optional<PasswordResetToken> existingToken = tokenRepository.findByUser(user);
        existingToken.ifPresent(tokenRepository::delete);

        // 2. Agora sim, cria um novo em folha
        String token = UUID.randomUUID().toString();
        PasswordResetToken myToken = new PasswordResetToken(
                token,
                user,
                LocalDateTime.now().plusMinutes(15) // Validade de 15 minutos
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

    // Envia o e-mail de fato usando a API do Brevo
    @Async
    public void sendResetEmail(String userEmail, String token) {
        // URLs do seu projeto no Render
        String resetUrl = "https://meu-backlog.onrender.com/resetar-senha?token=" + token;
        String logoUrl = "https://meu-backlog.onrender.com/img/logo.png?t=" + System.currentTimeMillis();

        // Montando o HTML do E-mail
        String htmlMsg = "<div style='font-family: Arial, sans-serif; background-color: #1a1a2e; color: #ffffff; padding: 40px 20px; text-align: center; border-radius: 10px; max-width: 600px; margin: 0 auto; border: 1px solid #333;'>"
                + "  <img src='" + logoUrl + "' alt='Meu Backlog Logo' style='max-width: 250px; margin-bottom: 20px;'>"
                + "  <h2 style='color: #ffffff; margin-bottom: 10px;'>Recuperação de Senha</h2>"
                + "  <p style='font-size: 16px; color: #cccccc; line-height: 1.5;'>Você solicitou a redefinição de sua senha.<br>Clique no botão abaixo para criar uma nova senha:</p>"
                + "  <a href='" + resetUrl
                + "' style='display: inline-block; padding: 14px 28px; margin: 25px 0; background-color: #e94560; color: #ffffff; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 16px;'>Redefinir Minha Senha</a>"
                + "  <p style='font-size: 14px; color: #888888; margin-top: 30px;'>Este link expira com segurança em 15 minutos.</p>"
                + "  <p style='font-size: 12px; color: #555555; margin-top: 10px;'>Se você não solicitou essa alteração, apenas ignore este e-mail.</p>"
                + "</div>";

        try {
            // Configurando a requisição HTTP
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);
            headers.set("accept", "application/json");

            // Montando o JSON que o Brevo exige
            String jsonBody = "{"
                    + "\"sender\": {\"name\": \"Meu Backlog\", \"email\": \"meusbacklog@gmail.com\"},"
                    + "\"to\": [{\"email\": \"" + userEmail + "\"}],"
                    + "\"subject\": \"Recuperação de Senha - Meu Backlog\","
                    + "\"htmlContent\": \"" + htmlMsg.replace("\"", "\\\"") + "\"" // Escapa as aspas pro JSON não quebrar
                    + "}";

            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

            // Disparando o e-mail pela porta 443
            restTemplate.postForEntity("https://api.brevo.com/v3/smtp/email", request, String.class);

            System.out.println("✅ E-mail disparado com sucesso via API Brevo para: " + userEmail);

        } catch (Exception e) {
            System.err.println("❌ ERRO AO ENVIAR E-MAIL VIA API BREVO: " + e.getMessage());
            e.printStackTrace();
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
