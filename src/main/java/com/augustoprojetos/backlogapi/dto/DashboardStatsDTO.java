package com.augustoprojetos.backlogapi.dto;

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
}