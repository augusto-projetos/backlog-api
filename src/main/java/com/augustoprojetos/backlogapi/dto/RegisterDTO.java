package com.augustoprojetos.backlogapi.dto;

public record RegisterDTO(String login, String password, String email, String socialUsername) {
    // Record é uma novidade do Java 17+ que cria getters/setters/construtor sozinho!
    // Perfeito para DTOs.
}
