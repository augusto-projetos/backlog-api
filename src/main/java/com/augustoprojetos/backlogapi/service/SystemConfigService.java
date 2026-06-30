package com.augustoprojetos.backlogapi.service;

import com.augustoprojetos.backlogapi.entity.SystemConfig;
import com.augustoprojetos.backlogapi.repository.SystemConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SystemConfigService {

    // Chaves conhecidas
    public static final String MODO_MANUTENCAO   = "MODO_MANUTENCAO";
    public static final String MODO_INSTAVEL     = "MODO_INSTAVEL";
    public static final String SISTEMA_BLOQUEADO = "SISTEMA_BLOQUEADO";
    public static final String MODO_READONLY     = "MODO_READONLY";
    public static final String NOVIDADES         = "NOVIDADES";

    @Autowired
    private SystemConfigRepository repository;

    public List<SystemConfig> listarTodas() {
        return repository.findAll();
    }

    public Map<String, String> mapaConfigs() {
        Map<String, String> mapa = new HashMap<>();
        repository.findAll().forEach(c -> mapa.put(c.getChave(), c.getValor()));
        return mapa;
    }

    public String get(String chave) {
        return repository.findById(chave)
                .map(SystemConfig::getValor)
                .orElse("false");
    }

    public boolean isAtivo(String chave) {
        return "true".equalsIgnoreCase(get(chave));
    }

    public boolean isBloqueado() {
        return isAtivo(SISTEMA_BLOQUEADO);
    }

    public boolean isModoManutencao() {
        return isAtivo(MODO_MANUTENCAO);
    }

    public boolean isModoInstavel() {
        return isAtivo(MODO_INSTAVEL);
    }

    public boolean isModoReadonly() {
        return isAtivo(MODO_READONLY);
    }

    public String getNovidadesTexto() {
        String v = get(NOVIDADES);
        return (v == null || v.isBlank()) ? null : v;
    }

    public void setValor(String chave, String valor) {
        SystemConfig config = repository.findById(chave)
                .orElse(new SystemConfig(chave, valor, null));
        config.setValor(valor);
        config.setAtualizadoEm(LocalDateTime.now());
        repository.save(config);
    }

    public void toggle(String chave) {
        boolean atual = isAtivo(chave);
        setValor(chave, atual ? "false" : "true");
    }
}
