package com.augustoprojetos.backlogapi.service;

import com.augustoprojetos.backlogapi.dto.tmdb.TmdbMovie;
import com.augustoprojetos.backlogapi.dto.tmdb.TmdbResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TmdbService {

    @Value("${tmdb.api.key:}")
    private String apiKey;

    @Value("${tmdb.api.url:https://api.themoviedb.org/3}")
    private String apiUrl;

    public List<SearchResult> buscarFilmes(String query) {
        RestTemplate restTemplate = new RestTemplate();

        String urlTemplate = UriComponentsBuilder.fromUriString(apiUrl)
                .path("/search/multi")
                .queryParam("api_key", apiKey)
                .queryParam("query", "{q}") // Placeholder de segurança
                .queryParam("language", "pt-BR")
                .queryParam("include_adult", "false")
                .encode()
                .toUriString();

        // Mapa para guardar o valor seguro
        Map<String, String> params = new HashMap<>();
        params.put("q", query);

        try {
            TmdbResponse response = restTemplate.getForObject(urlTemplate, TmdbResponse.class, params);

            if (response != null && response.results() != null) {
                return response.results().stream()
                        .filter(m -> m.poster_path() != null)
                        .map(m -> new SearchResult(
                                m.getTituloReal(),
                                "https://image.tmdb.org/t/p/w200" + m.poster_path(),
                                m.getDataReal()
                        ))
                        .limit(10)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Collections.emptyList();
    }

    // Uma classe simples (Record) só pra devolver pro Javascript
    public record SearchResult(String titulo, String imagem, String ano) {}
}