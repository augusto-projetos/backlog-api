package com.augustoprojetos.backlogapi.service;

import com.augustoprojetos.backlogapi.dto.tmdb.TmdbMovie;
import com.augustoprojetos.backlogapi.dto.tmdb.TmdbResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TmdbService {

    @Value("${tmdb.api.key:}")
    private String apiKey;

    @Value("${tmdb.api.url:https://api.themoviedb.org/3}")
    private String apiUrl;

    public List<SearchResult> buscarFilmes(String query) {
        RestTemplate restTemplate = new RestTemplate();

        // Monta a URL: https://api.themoviedb.org/3/search/multi?api_key=XXX&query=Batman
        String url = UriComponentsBuilder.fromUriString(apiUrl + "/search/multi")
                .queryParam("api_key", apiKey)
                .queryParam("query", query)
                .queryParam("language", "pt-BR")
                .queryParam("include_adult", "false")
                .toUriString();

        // Faz a chamada!
        try {
            TmdbResponse response = restTemplate.getForObject(url, TmdbResponse.class);

            if (response != null && response.results() != null) {
                // Filtra e transforma para um formato simples pro nosso Front
                return response.results().stream()
                        .filter(m -> m.poster_path() != null) // Ignora filmes sem capa
                        .map(m -> new SearchResult(
                                m.getTituloReal(),
                                "https://image.tmdb.org/t/p/w200" + m.poster_path(), // Monta a URL da imagem
                                m.getDataReal()
                        ))
                        .limit(10) // Só os top 10
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