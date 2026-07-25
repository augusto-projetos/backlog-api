package com.augustoprojetos.backlogapi.dto;

import com.augustoprojetos.backlogapi.entity.AtividadeLog;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    // Verdadeiro quando todos os eventos do grupo são sobre o mesmo item
    public boolean mesmoItem() {
        String primeiro = principal().getItemTitulo();
        if (primeiro == null) return false;
        return eventos.stream().allMatch(e -> primeiro.equals(e.getItemTitulo()));
    }

    // Lista (sem repetição) dos tipos de evento presentes no grupo,
    // separados por vírgula — usado para filtrar a timeline por tipo.
    public String tiposConcatenados() {
        return eventos.stream()
                .map(AtividadeLog::getTipo)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(","));
    }

    // Texto livre (título dos itens + descrições) usado na busca por
    // palavra-chave da timeline.
    public String textoBusca() {
        return eventos.stream()
                .flatMap(e -> Stream.of(e.getItemTitulo(), e.getDescricao()))
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" | "));
    }
}
