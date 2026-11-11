package com.augustoprojetos.backlogapi.dto;

/*
 * Todos os contadores que o sistema de conquistas precisa, calculados em UMA única query
 * agregada.
 */
public record EstatisticasConquistaDTO(
        long totalItens,
        long totalConcluidos,
        long totalDropados,
        long jogosZerados,
        long filmesAssistidos,
        long seriesAssistidas,
        long nota10Total,
        long filmesNota10,
        long jogosNota10,
        long itensComCapaTmdb,
        long minutosFilmesAssistidos,
        long minutosJogosTotal,
        long minutosJogoUnicoMax,
        long minutosSeriesAssistidas,
        long episodiosTotal,
        long episodiosSerieUnicaMax
) {

    /* Converte a linha do SELECT agregado (a ordem das colunas é a da query). SUM/MAX vêm null sem itens. */
    public static EstatisticasConquistaDTO de(Object[] linha) {
        return new EstatisticasConquistaDTO(
                n(linha[0]), n(linha[1]), n(linha[2]), n(linha[3]),
                n(linha[4]), n(linha[5]), n(linha[6]), n(linha[7]),
                n(linha[8]), n(linha[9]), n(linha[10]), n(linha[11]),
                n(linha[12]), n(linha[13]), n(linha[14]), n(linha[15]));
    }

    private static long n(Object valor) {
        return valor instanceof Number numero ? numero.longValue() : 0L;
    }
}
