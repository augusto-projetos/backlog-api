package com.augustoprojetos.backlogapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class EmailService {

    @Value("${BREVO_API_KEY}")
    private String brevoApiKey;

    @Async // Roda em segundo plano para não deixar a tela de cadastro travada carregando
    public void sendVerificationEmail(String userEmail, String token) {
        try {
            // Magic Link
            String verificationLink = "https://meus-backlog.onrender.com/auth/verify?token=" + token;

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

            dispararEmail(userEmail, "Verifique seu e-mail - Meus Backlog", htmlMsg);

            System.out.println("✅ E-mail de verificação disparado com sucesso para: " + userEmail);

        } catch (Exception e) {
            System.err.println("❌ ERRO AO ENVIAR E-MAIL DE VERIFICAÇÃO VIA API BREVO: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Dispara um comunicado/novidade em massa para uma lista de usuários.
    @Async
    public void sendBulkAnnouncementEmail(List<String> destinatarios, String mensagem) {
        String mensagemEscapada = escapeHtml(mensagem).replace("\n", "<br>");

        String htmlMsg = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;'>"
                + "<h2 style='color: #333;'>📢 Novidades do Meus Backlog</h2>"
                + "<p style='color: #444; line-height: 1.6;'>" + mensagemEscapada + "</p>"
                + "<p style='font-size: 12px; color: #999; margin-top: 30px;'>Você está recebendo este e-mail porque possui uma conta ativa no Meus Backlog.</p>"
                + "</div>";

        int enviados = 0;
        for (String destinatario : destinatarios) {
            try {
                dispararEmail(destinatario, "📢 Novidades - Meus Backlog", htmlMsg);
                enviados++;
                Thread.sleep(150); // pequeno intervalo para evitar rate-limit da API do Brevo
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                System.err.println("❌ Disparo em massa interrompido após " + enviados + " envios.");
                break;
            } catch (Exception e) {
                System.err.println("❌ Falha ao enviar comunicado para " + destinatario + ": " + e.getMessage());
            }
        }

        System.out.println("✅ Comunicado em massa finalizado: " + enviados + "/" + destinatarios.size() + " e-mails enviados.");
    }

    // Monta o payload e dispara uma única chamada para a API do Brevo
    private void dispararEmail(String destinatario, String assunto, String htmlMsg) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);
        headers.set("accept", "application/json");

        String jsonBody = "{"
                + "\"sender\": {\"name\": \"Meus Backlog\", \"email\": \"meusbacklog@gmail.com\"},"
                + "\"to\": [{\"email\": \"" + destinatario + "\"}],"
                + "\"subject\": \"" + assunto.replace("\"", "\\\"") + "\","
                + "\"htmlContent\": \"" + htmlMsg.replace("\"", "\\\"") + "\"" // Escapa as aspas
                + "}";

        HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
        restTemplate.postForEntity("https://api.brevo.com/v3/smtp/email", request, String.class);
    }

    // Escapa caracteres HTML para evitar quebra de layout/injeção quando o texto vem do admin
    private String escapeHtml(String texto) {
        return texto
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
