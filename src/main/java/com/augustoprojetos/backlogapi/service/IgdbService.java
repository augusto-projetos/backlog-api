package com.augustoprojetos.backlogapi.service;

import com.augustoprojetos.backlogapi.dto.igdb.IgdbGameResponse;
import com.augustoprojetos.backlogapi.dto.tmdb.TmdbMovie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class IgdbService {

    @Value("${igdb.client-id}")
    private String clientId;

    @Value("${igdb.auth-token}")
    private String authToken;

    private static final Logger log = LoggerFactory.getLogger(IgdbService.class);
    private static final String API_URL = "https://api.igdb.com/v4/games";

    // RestTemplate compartilhado (com timeouts) definido em HttpClientConfig
    private final RestTemplate restTemplate;

    public IgdbService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<TmdbMovie> buscarJogos(String query) {
        List<TmdbMovie> resultados = new ArrayList<>();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Client-ID", clientId);
            headers.set("Authorization", "bearer " + authToken);
            headers.setContentType(MediaType.TEXT_PLAIN);

            // Corpo de busca com a sintaxe oficial APICalypse da IGDB
            // Escapa \ e " do texto digitado: sem isso o usuário conseguia fechar a string e injetar
            // comandos na consulta APICalypse (ex.: alterar o limit ou os campos).
            String buscaSegura = query.replace("\\", "\\\\").replace("\"", "\\\"");
            String body = "search \"" + buscaSegura + "\"; fields name, cover.url, first_release_date; limit 10;";

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    API_URL,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            IgdbGameResponse[] jogos = mapper.readValue(responseEntity.getBody(), IgdbGameResponse[].class);

            if (jogos != null) {
                for (IgdbGameResponse game : jogos) {
                    String capaUrl = "https://placehold.co/150x200?text=Sem+Capa";
                    if (game.getCover() != null && game.getCover().getUrl() != null) {
                        capaUrl = game.getCover().getUrl().replace("t_thumb", "t_cover_big");
                        if (capaUrl.startsWith("//")) {
                            capaUrl = "https:" + capaUrl;
                        }
                    }

                    // Formata a data de lançamento para o padrão "YYYY-MM-DD" ou usa uma data padrão se não houver
                    String dataFormatada = "";
                    if (game.getFirstReleaseDate() != null) {
                        java.time.Instant instant = java.time.Instant.ofEpochSecond(game.getFirstReleaseDate());
                        java.time.ZonedDateTime data = instant.atZone(java.time.ZoneId.systemDefault());
                        dataFormatada = data.getYear() + "-01-01";
                    }

                    TmdbMovie movieRecord = new TmdbMovie(
                            null, // Jogos não usam o id do TMDB
                            game.getName(),
                            game.getName(),
                            game.getName(),
                            dataFormatada,
                            dataFormatada,
                            capaUrl
                    );
                    resultados.add(movieRecord);
                }
            }
        } catch (Exception e) {
            log.warn("[IGDB] Falha na busca: {}", e.getMessage());
        }
        return resultados;
    }
}
