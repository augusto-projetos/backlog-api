package com.augustoprojetos.backlogapi.service;

import com.augustoprojetos.backlogapi.dto.ConquistaDesbloqueadaDTO;
import com.augustoprojetos.backlogapi.entity.Item;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.repository.ItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
 * Recomendações por IA com cadeia de contingência:
 *   modelos Gemini (em ordem)  →  modelos Groq (em ordem)  →  mensagem amigável.
 */
@Service
public class RecomendacaoService {

    private static final Logger log = LoggerFactory.getLogger(RecomendacaoService.class);

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    // Instabilidade transitória (503 "alta demanda", 429...) merece uma segunda chance no mesmo modelo
    private static final int TENTATIVAS_POR_MODELO = 2;
    private static final long PAUSA_ENTRE_TENTATIVAS_MS = 800;
    // Teto para a cadeia inteira, para o usuário não ficar esperando indefinidamente
    private static final Duration PRAZO_TOTAL = Duration.ofSeconds(45);

    private static final String CONQUISTA_MENTE_EXPANDIDA = "MENTE_EXPANDIDA";
    private static final String MENSAGEM_FALHA_GERAL =
            "🤖 Ops! Os motores de IA estão instáveis no momento. Tente de novo em instantes!";

    private final ItemRepository itemRepository;
    private final ConquistaService conquistaService;
    private final AtividadeLogService atividadeLogService;
    private final String geminiApiKey;
    private final String groqApiKey;
    private final List<String> modelosGemini;
    private final List<String> modelosGroq;
    private final RestTemplate restTemplate;

    public RecomendacaoService(ItemRepository itemRepository,
                               ConquistaService conquistaService,
                               AtividadeLogService atividadeLogService,
                               @Value("${GEMINI_API_KEY}") String geminiApiKey,
                               @Value("${GROQ_API_KEY}") String groqApiKey,
                               @Value("${ia.gemini.modelos}") List<String> modelosGemini,
                               @Value("${ia.groq.modelos}") List<String> modelosGroq) {
        this.itemRepository = itemRepository;
        this.conquistaService = conquistaService;
        this.atividadeLogService = atividadeLogService;
        this.geminiApiKey = geminiApiKey;
        this.groqApiKey = groqApiKey;
        this.modelosGemini = modelosGemini.stream().map(String::trim).filter(m -> !m.isEmpty()).toList();
        this.modelosGroq = modelosGroq.stream().map(String::trim).filter(m -> !m.isEmpty()).toList();

        // Modelos de IA respondem em segundos: o timeout de leitura é maior que o das APIs de busca
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(5));
        fabrica.setReadTimeout(Duration.ofSeconds(25));
        this.restTemplate = new RestTemplate(fabrica);
    }

    public record RecomendacaoResult(String recomendacao, ConquistaDesbloqueadaDTO conquistaDesbloqueada) {}

    public RecomendacaoResult obterRecomendacaoDaIA(User user, String tipoSolicitado) {
        String prompt = gerarPromptDoUsuario(user, tipoSolicitado);
        long limite = System.nanoTime() + PRAZO_TOTAL.toNanos();

        String texto = tentarModelos("Gemini", modelosGemini, limite, modelo -> chamarGemini(modelo, prompt));
        if (texto == null) {
            log.warn("[IA] Gemini indisponível, acionando contingência na Groq");
            texto = tentarModelos("Groq", modelosGroq, limite, modelo -> chamarGroq(modelo, prompt));
        }
        if (texto == null) {
            log.error("[IA] Todos os motores falharam");
            return new RecomendacaoResult(MENSAGEM_FALHA_GERAL, null);
        }

        // A conquista é verificada uma vez, só depois de ter a resposta.
        return new RecomendacaoResult(texto, desbloquearConquista(user));
    }

    /** Percorre os modelos em ordem e devolve o primeiro texto obtido (ou null se todos falharem). */
    private String tentarModelos(String provedor, List<String> modelos, long limiteNanos,
                                 Function<String, String> chamada) {
        for (String modelo : modelos) {
            for (int tentativa = 1; tentativa <= TENTATIVAS_POR_MODELO; tentativa++) {
                if (System.nanoTime() > limiteNanos) {
                    log.warn("[IA] Prazo total esgotado antes de concluir a cadeia ({})", provedor);
                    return null;
                }
                try {
                    String texto = chamada.apply(modelo);
                    if (texto != null && !texto.isBlank()) {
                        if (tentativa > 1) {
                            log.info("[IA] {} / {} respondeu na tentativa {}", provedor, modelo, tentativa);
                        }
                        return texto;
                    }
                    log.warn("[IA] {} / {} devolveu resposta vazia", provedor, modelo);
                    break; // resposta vazia não melhora repetindo: próximo modelo
                } catch (HttpStatusCodeException e) {
                    int status = e.getStatusCode().value();
                    log.warn("[IA] {} / {} -> HTTP {} (tentativa {}/{}): {}", provedor, modelo, status,
                            tentativa, TENTATIVAS_POR_MODELO, resumir(e.getResponseBodyAsString()));
                    // 404/400/401/403 etc. não se resolvem sozinhos (modelo aposentado, chave inválida): pula o modelo.
                    if (!ehTransitorio(status)) break;
                } catch (ResourceAccessException e) {
                    log.warn("[IA] {} / {} sem resposta (tentativa {}/{}): {}", provedor, modelo,
                            tentativa, TENTATIVAS_POR_MODELO, e.getMessage());
                } catch (RuntimeException e) {
                    log.warn("[IA] {} / {} falhou: {}", provedor, modelo, e.getMessage());
                    break;
                }

                if (tentativa < TENTATIVAS_POR_MODELO && !pausar()) {
                    return null; // thread interrompida
                }
            }
        }
        return null;
    }

    private boolean ehTransitorio(int status) {
        return status == 429 || status == 500 || status == 502 || status == 503 || status == 504;
    }

    private boolean pausar() {
        try {
            Thread.sleep(PAUSA_ENTRE_TENTATIVAS_MS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private String resumir(String corpo) {
        if (corpo == null) return "";
        String limpo = corpo.replaceAll("\\s+", " ").trim();
        return limpo.length() > 200 ? limpo.substring(0, 200) + "…" : limpo;
    }

    // --- Gemini ---

    private String chamarGemini(String modelo, String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", geminiApiKey);

        Map<String, Object> corpo = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));

        ResponseEntity<Map> resposta = restTemplate.exchange(
                String.format(GEMINI_URL, modelo), HttpMethod.POST, new HttpEntity<>(corpo, headers), Map.class);

        return extrairTextoGemini(resposta.getBody());
    }

    // Junta todas as partes de texto da resposta (ignorando partes de "pensamento" dos modelos 3.x)
    private String extrairTextoGemini(Map<?, ?> corpo) {
        if (corpo == null || !(corpo.get("candidates") instanceof List<?> candidatos) || candidatos.isEmpty()) {
            return null;
        }
        if (!(candidatos.get(0) instanceof Map<?, ?> candidato)
                || !(candidato.get("content") instanceof Map<?, ?> conteudo)
                || !(conteudo.get("parts") instanceof List<?> partes)) {
            return null;
        }
        StringBuilder texto = new StringBuilder();
        for (Object parte : partes) {
            if (parte instanceof Map<?, ?> p
                    && !Boolean.TRUE.equals(p.get("thought"))
                    && p.get("text") instanceof String trecho) {
                texto.append(trecho);
            }
        }
        return texto.toString();
    }

    // --- Groq (API compatível com OpenAI) ---

    private String chamarGroq(String modelo, String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> corpo = Map.of(
                "model", modelo,
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.7);

        ResponseEntity<Map> resposta = restTemplate.exchange(
                GROQ_URL, HttpMethod.POST, new HttpEntity<>(corpo, headers), Map.class);

        Map<?, ?> body = resposta.getBody();
        if (body == null || !(body.get("choices") instanceof List<?> escolhas) || escolhas.isEmpty()) {
            return null;
        }
        if (escolhas.get(0) instanceof Map<?, ?> escolha
                && escolha.get("message") instanceof Map<?, ?> mensagem
                && mensagem.get("content") instanceof String conteudo) {
            return conteudo;
        }
        return null;
    }

    // --- Conquista ---

    private ConquistaDesbloqueadaDTO desbloquearConquista(User user) {
        try {
            ConquistaDesbloqueadaDTO conquista = conquistaService.desbloquearEvento(user, CONQUISTA_MENTE_EXPANDIDA);
            if (conquista != null) {
                atividadeLogService.registrarConquistaDesbloqueada(user, conquista.nome(), conquista.icone());
            }
            return conquista;
        } catch (RuntimeException e) {
            log.warn("[IA] Falha ao registrar a conquista {}: {}", CONQUISTA_MENTE_EXPANDIDA, e.getMessage());
            return null;
        }
    }

    private String gerarPromptDoUsuario(User user, String tipoSolicitado) {
        List<Item> acervo = itemRepository.findByUser(user);

        String favoritos = acervo.stream()
                .filter(item -> item.getNota() != null && item.getNota() >= 8.0)
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
