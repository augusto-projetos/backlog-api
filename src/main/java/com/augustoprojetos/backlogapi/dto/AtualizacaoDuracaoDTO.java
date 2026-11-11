package com.augustoprojetos.backlogapi.dto;

/* Item do corpo de PUT /itens/duracao-lote: qual filme e quantos minutos ele dura. */
public record AtualizacaoDuracaoDTO(Long id, Integer duracaoMinutos) {}
