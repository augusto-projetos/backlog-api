package com.augustoprojetos.backlogapi;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class BacklogApiApplication {

    @PostConstruct
    public void init() {
        // Força a JVM usar o fuso horário de Brasília
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
    }

    public static void main(String[] args) {
        // 1. Carrega o arquivo .env
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        // 2. Transforma as variáveis do .env em propriedades padrão do Spring
        Map<String, Object> dotenvProperties = new HashMap<>();
        dotenv.entries().forEach(entry -> {
            dotenvProperties.put(entry.getKey(), entry.getValue());
            System.setProperty(entry.getKey(), entry.getValue());
        });

        // 3. Inicializa o Spring injetando o mapa de propriedades antes de ler o application.properties
        SpringApplication app = new SpringApplication(BacklogApiApplication.class);
        app.setDefaultProperties(dotenvProperties);
        app.run(args);
    }

}
