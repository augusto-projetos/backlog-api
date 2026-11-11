package com.augustoprojetos.backlogapi.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/*
 * RestTemplate compartilhado para APIs externas de busca (TMDB e IGDB), com timeouts.
 */
@Configuration
public class HttpClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(6);

    @Bean
    public RestTemplate buscaExternaRestTemplate() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(CONNECT_TIMEOUT);
        fabrica.setReadTimeout(READ_TIMEOUT);
        return new RestTemplate(fabrica);
    }
}
