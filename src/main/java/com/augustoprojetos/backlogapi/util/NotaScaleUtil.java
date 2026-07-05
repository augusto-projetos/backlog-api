package com.augustoprojetos.backlogapi.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * Utilitário para montar o mapa "nota -> quantidade" usado no gráfico de
 * Distribuição de Notas. Mostra somente as notas que realmente têm itens
 * avaliados, sempre ordenadas da maior para a menor (10 -> 0).
 */
public final class NotaScaleUtil {

    private NotaScaleUtil() {
    }

    // Formata a nota removendo o ".0" quando ela for inteira (10.0 -> "10", 9.5 -> "9.5")
    public static String formatarNota(double nota) {
        return (nota == Math.floor(nota)) ? String.valueOf((int) nota) : String.valueOf(nota);
    }

    /*
     * Monta o mapa nota -> quantidade apenas com as notas que têm itens
     * (sem preencher com zero as notas vazias), ordenado da maior para a menor.
     *
     * @param contagemPorNota mapa com a contagem real por nota
     */
    public static Map<String, Long> apenasComItens(Map<Double, Long> contagemPorNota) {
        return contagemPorNota.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getKey(), a.getKey()))
                .collect(Collectors.toMap(
                        e -> formatarNota(e.getKey()),
                        Map.Entry::getValue,
                        (x, y) -> x,
                        LinkedHashMap::new));
    }
}
