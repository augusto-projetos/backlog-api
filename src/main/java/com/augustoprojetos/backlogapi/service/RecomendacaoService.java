package com.augustoprojetos.backlogapi.service;

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

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String obterRecomendacaoDaIA(User user, String tipoSolicitado) {
        String promptCompleto = gerarPromptDoUsuario(user, tipoSolicitado);

        try {
            // Endereço completo com a chave de autenticação na Query URL
            String urlComKey = apiUrl + "?key=" + apiKey;

            // Monta o corpo do JSON exatamente no padrão exigido pela API do Gemini v1beta
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

            // Dispara a requisição POST para o Google
            ResponseEntity<Map> response = restTemplate.exchange(urlComKey, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Navega de forma segura pela árvore do JSON retornado pelo Gemini para capturar o texto
                List<?> candidates = (List<?>) response.getBody().get("candidates");
                Map<?, ?> firstCandidate = (Map<?, ?>) candidates.get(0);
                Map<?, ?> content = (Map<?, ?>) firstCandidate.get("content");
                List<?> parts = (List<?>) content.get("parts");
                Map<?, ?> firstPart = (Map<?, ?>) parts.get(0);
                
                return (String) firstPart.get("text");
            }
            
            return "🤖 O Geminino recebeu uma resposta inesperada dos servidores centrais. Tente de novo!";

        } catch (Exception e) {
            // Captura falhas de timeout, chaves incorretas ou falta de internet
            return "🤖 Ops! O Geminino deu uma cochilada e não conseguiu se conectar ao servidor de inteligência agora.";
        }
    }

    private String gerarPromptDoUsuario(User user, String tipoSolicitado) {
        List<Item> acervo = itemRepository.findByUser(user);

        // 2. Extrai os favoritos (Notas altas de 8.0 a 10.0) filtrados por tipo ou de forma geral
        String favoritos = acervo.stream()
                .filter(item -> item.getNota() >= 8.0)
                .map(item -> item.getTitulo() + " (" + item.getTipo() + ") - Nota: " + item.getNota())
                .collect(Collectors.joining(", "));

        // 3. Extrai o que ele está consumindo atualmente ("andamento")
        String emAndamento = acervo.stream()
                .filter(item -> "andamento".equalsIgnoreCase(item.getStatus()))
                .map(Item::getTitulo)
                .collect(Collectors.joining(", "));

        // 4. Extrai o que ele planeja consumir no futuro ("backlog")
        String listaDesejos = acervo.stream()
                .filter(item -> "backlog".equalsIgnoreCase(item.getStatus()))
                .map(Item::getTitulo)
                .collect(Collectors.joining(", "));

        // Se o usuário não tiver nada cadastrado ainda, damos um fallback seguro para não quebrar a IA
        if (favoritos.isEmpty() && emAndamento.isEmpty()) {
            favoritos = "Nenhum item cadastrado ainda. O usuário é novo na plataforma.";
        }

        // 5. Engenharia de Prompt: Monta a instrução rígida e estruturada para a IA
        StringBuilder prompt = new StringBuilder();
        prompt.append("Você é um especialista em entretenimento e o motor de recomendações do aplicativo 'Meus Backlog'.\n");
        prompt.append("Analise o perfil de consumo do usuário abaixo e sugira exatamente 3 recomendações inéditas de ");
        prompt.append(tipoSolicitado.toUpperCase()).append(" que combinem perfeitamente com o gosto dele.\n\n");
        
        prompt.append("--- PERFIL DO USUÁRIO ---\n");
        prompt.append("Mídias bem avaliadas pelo usuário: [ ").append(favoritos).append(" ]\n");
        prompt.append("Mídias que ele está assistindo/jogando agora: [ ").append(emAndamento).append(" ]\n");
        prompt.append("Mídias que estão na lista de espera (Backlog): [ ").append(listaDesejos).append(" ]\n\n");
        
        prompt.append("--- REGRAS DE RESPOSTA ---\n");
        prompt.append("1. Recomende APENAS itens do tipo: ").append(tipoSolicitado).append(".\n");
        prompt.append("2. Não recomende mídias que já estejam listadas no perfil do usuário.\n");
        prompt.append("3. Para cada recomendação, forneça o Título e uma justificativa curta e empolgante de até 3 linhas explicando o porquê da escolha baseado nos gostos dele.\n");
        prompt.append("4. Escreva em português brasileiro com um tom amigável e descontraído de um amigo gamer/cinéfilo.\n");
        prompt.append("5. Responda em um formato limpo, usando marcadores claros para cada uma das 3 sugestões.");

        return prompt.toString();
    }
}