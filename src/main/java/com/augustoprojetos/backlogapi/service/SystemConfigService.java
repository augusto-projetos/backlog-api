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

    // Tempo máximo que uma leitura pode ficar desatualizada (só importa se o banco for alterado por fora da aplicação)
    private static final long VALIDADE_CACHE_MS = 5_000;

    @Autowired
    private SystemConfigRepository repository;

    private final Object trava = new Object();
    private volatile Map<String, String> cache = Map.of();
    private volatile long carregadoEm = 0L;

    public List<SystemConfig> listarTodas() {
        return repository.findAll();
    }

    public Map<String, String> mapaConfigs() {
        return new HashMap<>(configs());
    }

    public String get(String chave) {
        String valor = configs().get(chave);
        return valor != null ? valor : "false";
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
        invalidarCache();
    }

    public void toggle(String chave) {
        invalidarCache();
        boolean atual = isAtivo(chave);
        setValor(chave, atual ? "false" : "true");
    }

    // --- Cache ---

    private Map<String, String> configs() {
        long agora = System.currentTimeMillis();
        if (agora - carregadoEm < VALIDADE_CACHE_MS) {
            return cache;
        }
        synchronized (trava) {
            if (System.currentTimeMillis() - carregadoEm < VALIDADE_CACHE_MS) {
                return cache;
            }
            Map<String, String> recarregado = new HashMap<>();
            repository.findAll().forEach(c -> recarregado.put(c.getChave(), c.getValor()));
            cache = Map.copyOf(recarregado);
            carregadoEm = System.currentTimeMillis();
            return cache;
        }
    }

    private void invalidarCache() {
        carregadoEm = 0L;
    }
}
