package com.augustoprojetos.backlogapi.dto;

import java.time.LocalDate;
import java.util.List;

// Representa um "dia" na Linha do Tempo.
public record TimelineDiaDTO(
        String rotulo,
        LocalDate data,
        List<TimelineGrupoDTO> grupos
) {
}
