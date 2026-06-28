package com.augustoprojetos.backlogapi.dto.admin;

import lombok.Data;
import java.util.Map;

@Data
public class AdminGlobalStatsDTO {
    private long totalUsuarios;
    private long totalPendentes;
    private long totalFilmes;
    private long totalSeries;
    private long totalJogos;
    private long totalItens;
    private long totalAssistidos;
    private long totalAssistindo;
    private long totalBacklog;
    private long totalDropados;
    private Map<String, Long> distribNotas;
}
