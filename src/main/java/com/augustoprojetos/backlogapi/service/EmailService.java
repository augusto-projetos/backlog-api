package com.augustoprojetos.backlogapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class EmailService {

    @Value("${BREVO_API_KEY}")
    private String brevoApiKey;

    @Async // Roda em segundo plano para não deixar a tela de cadastro travada carregando
    public void sendVerificationEmail(String userEmail, String token) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);
            headers.set("accept", "application/json");

            // Magic Link (ajuste a porta/domínio se necessário)
            String verificationLink = "http://localhost:8080/auth/verify?token=" + token;

            // Corpo do e-mail em HTML
            String htmlMsg = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;'>"
                    + "<h2 style='color: #333;'>Bem-vindo ao Meus Backlog! 🚀</h2>"
                    + "<p>Falta pouco para você começar a organizar seus projetos. Para ativar sua conta e acessar o sistema, clique no botão abaixo:</p>"
                    + "<div style='text-align: center; margin: 30px 0;'>"
                    + "<a href='" + verificationLink + "' style='background-color: #4CAF50; color: white; padding: 12px 20px; text-decoration: none; border-radius: 5px; font-weight: bold;'>Verificar e Entrar</a>"
                    + "</div>"
                    + "<p style='font-size: 14px; color: #777;'>Ou copie e cole o link abaixo no seu navegador:</p>"
                    + "<p style='font-size: 14px; color: #4CAF50; word-break: break-all;'>" + verificationLink + "</p>"
                    + "<p style='font-size: 12px; color: #999; margin-top: 30px;'>*Este link expira em 24 horas. Se você não criou esta conta, apenas ignore este e-mail.</p>"
                    + "</div>";

            // Montando o JSON
            String jsonBody = "{"
                    + "\"sender\": {\"name\": \"Meus Backlog\", \"email\": \"meusbacklog@gmail.com\"},"
                    + "\"to\": [{\"email\": \"" + userEmail + "\"}],"
                    + "\"subject\": \"Verifique seu e-mail - Meus Backlog\","
                    + "\"htmlContent\": \"" + htmlMsg.replace("\"", "\\\"") + "\"" // Escapa as aspas
                    + "}";

            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

            // Disparando o e-mail
            restTemplate.postForEntity("https://api.brevo.com/v3/smtp/email", request, String.class);

            System.out.println("✅ E-mail de verificação disparado com sucesso para: " + userEmail);

        } catch (Exception e) {
            System.err.println("❌ ERRO AO ENVIAR E-MAIL DE VERIFICAÇÃO VIA API BREVO: " + e.getMessage());
            e.printStackTrace();
        }
    }
}