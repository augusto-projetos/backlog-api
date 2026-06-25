package com.augustoprojetos.backlogapi.dto;

public record ConquistaDesbloqueadaDTO(
    String chave,
    String nome,
    String descricao,
    String icone,
    int xp
) {}