package com.augustoprojetos.backlogapi.service;

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

    @Value("${TMDB_API_KEY}")
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

    // 🎬 Busca automaticamente quantos minutos um filme dura, para alimentar o
    // gráfico de "Tempo Gasto" do dashboard. Faz uma busca simples pelo título
    public Integer buscarDuracaoFilme(String titulo) {
        if (titulo == null || titulo.isBlank()) return null;

        RestTemplate restTemplate = new RestTemplate();

        try {
            // 1. Busca o filme pelo título para descobrir o ID dele no TMDB
            String urlBusca = UriComponentsBuilder.fromUriString(apiUrl)
                    .path("/search/movie")
                    .queryParam("api_key", apiKey)
                    .queryParam("query", "{q}")
                    .queryParam("language", "pt-BR")
                    .queryParam("include_adult", "false")
                    .encode()
                    .toUriString();

            Map<String, String> params = new HashMap<>();
            params.put("q", titulo);

            TmdbResponse resultadoBusca = restTemplate.getForObject(urlBusca, TmdbResponse.class, params);

            if (resultadoBusca == null || resultadoBusca.results() == null || resultadoBusca.results().isEmpty()) {
                return null;
            }

            Long filmeId = resultadoBusca.results().get(0).id();
            if (filmeId == null) return null;

            // 2. Com o ID em mãos, busca os detalhes (que incluem a duração em minutos)
            String urlDetalhes = UriComponentsBuilder.fromUriString(apiUrl)
                    .path("/movie/{id}")
                    .queryParam("api_key", apiKey)
                    .queryParam("language", "pt-BR")
                    .encode()
                    .toUriString();

            Map<String, String> paramsId = new HashMap<>();
            paramsId.put("id", String.valueOf(filmeId));

            @SuppressWarnings("unchecked")
            Map<String, Object> detalhes = restTemplate.getForObject(urlDetalhes, Map.class, paramsId);

            if (detalhes != null && detalhes.get("runtime") != null) {
                Object runtime = detalhes.get("runtime");
                if (runtime instanceof Number numero && numero.intValue() > 0) {
                    return numero.intValue();
                }
            }
        } catch (Exception e) {
            System.err.println("[TMDB] Não foi possível buscar a duração de \"" + titulo + "\": " + e.getMessage());
        }

        return null;
    }

    // Uma classe simples (Record) só pra devolver pro Javascript
    public record SearchResult(String titulo, String imagem, String ano) {}
}
