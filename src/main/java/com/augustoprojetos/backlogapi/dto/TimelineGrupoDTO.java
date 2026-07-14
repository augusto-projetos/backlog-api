package com.augustoprojetos.backlogapi.dto;

import com.augustoprojetos.backlogapi.entity.AtividadeLog;

import java.util.List;

/*
 * Representa um "bloco" da Linha do Tempo: um ou mais {@link AtividadeLog}
 * que aconteceram juntos, na mesma ação do usuário (mesmo grupoId).
 *
 * Quando tem só 1 evento, a timeline exibe normalmente.
 * Quando tem mais de 1, a timeline mostra um resumo com uma seta para
 * expandir e ver cada evento em detalhe.
 */
public record TimelineGrupoDTO(
        String grupoId,
        List<AtividadeLog> eventos
) {
    public AtividadeLog principal() {
        return eventos.get(0);
    }

    public boolean multiplo() {
        return eventos.size() > 1;
    }

    public int quantidade() {
        return eventos.size();
    }
}
