package com.augustoprojetos.backlogapi.dto;

import java.util.Map;

public class DashboardStatsDTO {

    // Contagem por TIPO
    private long totalJogos;
    private long totalFilmes;
    private long totalSeries;

    // Contagem por STATUS
    private long totalZerados;
    private long totalJogando;
    private long totalBacklog;
    private long totalDropados;

    // Mapa de Notas
    private Map<String, Long> notas;

    // --- Tempo Gasto (em minutos) ---
    private long minutosFilmes;
    private long minutosJogos;
    // Séries ainda não entram na conta: a funcionalidade estará disponível em breve
    private boolean tempoSeriesDisponivel = false;

    // Getters e Setters
    public long getTotalJogos() { return totalJogos; }
    public void setTotalJogos(long totalJogos) { this.totalJogos = totalJogos; }

    public long getTotalFilmes() { return totalFilmes; }
    public void setTotalFilmes(long totalFilmes) { this.totalFilmes = totalFilmes; }

    public long getTotalSeries() { return totalSeries; }
    public void setTotalSeries(long totalSeries) { this.totalSeries = totalSeries; }

    public long getTotalZerados() { return totalZerados; }
    public void setTotalZerados(long totalZerados) { this.totalZerados = totalZerados; }

    public long getTotalJogando() { return totalJogando; }
    public void setTotalJogando(long totalJogando) { this.totalJogando = totalJogando; }

    public long getTotalBacklog() { return totalBacklog; }
    public void setTotalBacklog(long totalBacklog) { this.totalBacklog = totalBacklog; }

    public long getTotalDropados() { return totalDropados; }
    public void setTotalDropados(long totalDropados) { this.totalDropados = totalDropados; }

    public Map<String, Long> getNotas() { return notas; }
    public void setNotas(Map<String, Long> notas) { this.notas = notas; }

    public long getMinutosFilmes() { return minutosFilmes; }
    public void setMinutosFilmes(long minutosFilmes) { this.minutosFilmes = minutosFilmes; }

    public long getMinutosJogos() { return minutosJogos; }
    public void setMinutosJogos(long minutosJogos) { this.minutosJogos = minutosJogos; }

    public boolean isTempoSeriesDisponivel() { return tempoSeriesDisponivel; }
    public void setTempoSeriesDisponivel(boolean tempoSeriesDisponivel) { this.tempoSeriesDisponivel = tempoSeriesDisponivel; }
}
