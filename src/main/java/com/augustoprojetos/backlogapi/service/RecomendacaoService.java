package com.augustoprojetos.backlogapi.service;

import com.augustoprojetos.backlogapi.dto.ConquistaDesbloqueadaDTO;
import com.augustoprojetos.backlogapi.entity.Item;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RecomendacaoService {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ConquistaService conquistaService;

    @Autowired
    private AtividadeLogService atividadeLogService;

    @Value("${GEMINI_API_KEY}")
    private String geminiApiKey;

    @Value("${GROQ_API_KEY}")
    private String groqApiKey;

    // URL do motor principal (Google Gemini 2.5 Flash)
    private final String geminiFlashUrl = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent";

    // URL do motor de failover estável e gratuito (Groq API)
    private final String groqApiUrl = "https://api.groq.com/openai/v1/chat/completions";

    private final RestTemplate restTemplate = new RestTemplate();

    public record RecomendacaoResult(String recomendacao, ConquistaDesbloqueadaDTO conquistaDesbloqueada) {}

    public RecomendacaoResult obterRecomendacaoDaIA(User user, String tipoSolicitado) {
        String promptCompleto = gerarPromptDoUsuario(user, tipoSolicitado);

        try {
            // 1. TENTATIVA PRINCIPAL: Google Gemini 2.5 Flash
            String urlComKey = geminiFlashUrl + "?key=" + geminiApiKey;

            Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(
                        Map.of("text", promptCompleto)
                    ))
                )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(urlComKey, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<?> candidates = (List<?>) response.getBody().get("candidates");
                Map<?, ?> firstCandidate = (Map<?, ?>) candidates.get(0);
                Map<?, ?> content = (Map<?, ?>) firstCandidate.get("content");
                List<?> parts = (List<?>) content.get("parts");
                Map<?, ?> firstPart = (Map<?, ?>) parts.get(0);

                // GATILHO DA CONQUISTA: Se a IA respondeu, dispara a checagem da chave
                ConquistaDesbloqueadaDTO conquista = conquistaService.desbloquearEvento(user, "MENTE_EXPANDIDA");
                if (conquista != null) {
                    atividadeLogService.registrarConquistaDesbloqueada(user, conquista.nome(), conquista.icone());
                }

                return new RecomendacaoResult((String) firstPart.get("text"), conquista);
            }
            throw new RuntimeException("Resposta inválida do Gemini");

        } catch (Exception e) {
            // O Gemini falhou! O catch captura e joga imediatamente para a Groq (Meta Llama 3)
            System.out.println("⚠️ O motor principal (Gemini 2.5 Flash) falhou devido a: " + e.getMessage());
            System.out.println("🚀 Acionando plano de contingência: Mudando para Groq Cloud (Llama 3)...");

            return chamarGroqFallback(promptCompleto, user);
        }
    }

    // Motor secundário de failover utilizando a API Gratuita da Groq Cloud
    private RecomendacaoResult chamarGroqFallback(String prompt, User user) {
        try {
            // Corpo JSON atualizado com o modelo Llama 3.1 ativo na Groq
            Map<String, Object> requestBody = Map.of(
                "model", "llama-3.1-8b-instant",
                "messages", List.of(
                    Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.7
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey); // Passa a gsk_ key no Bearer Token

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(groqApiUrl, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<?> choices = (List<?>) response.getBody().get("choices");
                Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
                Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");

                // GATILHO DA CONQUISTA NO FALLBACK
                ConquistaDesbloqueadaDTO conquista = conquistaService.desbloquearEvento(user, "MENTE_EXPANDIDA");
                if (conquista != null) {
                    atividadeLogService.registrarConquistaDesbloqueada(user, conquista.nome(), conquista.icone());
                }

                return new RecomendacaoResult((String) message.get("content"), conquista);
            }

            return new RecomendacaoResult("🤖 Ops! O Gemini e o Llama tentaram, mas os servidores de IA estão instáveis. Tente de novo!", null);

        } catch (Exception ex) {
            ex.printStackTrace();
            return new RecomendacaoResult("🤖 Ocorreu uma falha geral nos motores de recomendação por IA.", null);
        }
    }

    private String gerarPromptDoUsuario(User user, String tipoSolicitado) {
        List<Item> acervo = itemRepository.findByUser(user);

        String favoritos = acervo.stream()
                .filter(item -> item.getNota() >= 8.0)
                .map(item -> item.getTitulo() + " (" + item.getTipo() + ") - Nota: " + item.getNota())
                .collect(Collectors.joining(", "));

        String emAndamento = acervo.stream()
                .filter(item -> "andamento".equalsIgnoreCase(item.getStatus()))
                .map(Item::getTitulo)
                .collect(Collectors.joining(", "));

        String listaDesejos = acervo.stream()
                .filter(item -> "backlog".equalsIgnoreCase(item.getStatus()))
                .map(Item::getTitulo)
                .collect(Collectors.joining(", "));

        if (favoritos.isEmpty() && emAndamento.isEmpty()) {
            favoritos = "Nenhum item cadastrado ainda. O usuário é novo na plataforma.";
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("Você é um especialista em entretenimento e o motor de recomendações do aplicativo 'Meus Backlog'.\n");
        prompt.append("Analise o perfil de consumo do usuário abaixo e sugira exatamente 3 recomendações inéditas de ");
        prompt.append(tipoSolicitado.toUpperCase()).append(" que combinem perfeitamente com o gosto dele.\n\n");

        prompt.append("--- PERFIL DO USUÁRIO ---\n");
        prompt.append("Mídias bem avaliadas pelo usuário: [ ").append(favoritos).append(" ]\n");
        prompt.append("Mídias que ele está assistindo/jogando agora: [ ").append(emAndamento).append(" ]\n");
        prompt.append("Mídias que estão na lista de espera (Backlog): [ ").append(listaDesejos).append(" ]\n\n");

        prompt.append("--- REGRAS DE RESPOSTA E FORMATAÇÃO VISUAL ---\n");
        prompt.append("1. Recomende APENAS itens do tipo: ").append(tipoSolicitado).append(".\n");
        prompt.append("2. Não recomende mídias que já estejam listadas no perfil do usuário.\n");
        prompt.append("3. Adote OBRIGATORIAMENTE a seguinte estrutura visual para cada uma das 3 recomendações:\n");
        prompt.append("   **[Número]• [Título da Mídia]**\n");
        prompt.append("   *Justificativa:* [Escreva aqui o motivo empolgante focado nos gostos dele em até 3 linhas]\n\n");
        prompt.append("4. Separe cada uma das 3 recomendações por uma linha em branco para o texto respirar na tela.\n");
        prompt.append("5. Insira uma saudação rápida e amigável com um emoji no início e uma frase descontraída de encerramento no final.\n");
        prompt.append("6. Escreva em português brasileiro, mantendo um tom de um amigo cinéfilo/gamer.");

        return prompt.toString();
    }
}
