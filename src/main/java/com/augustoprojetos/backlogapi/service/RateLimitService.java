package com.augustoprojetos.backlogapi.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    // Guarda quantas vezes o e-mail tentou
    private final Map<String, Integer> tentativas = new ConcurrentHashMap<>();
    // Guarda o horário da primeira tentativa
    private final Map<String, Long> tempos = new ConcurrentHashMap<>();

    private final int MAX_TENTATIVAS = 3;
    private final long TEMPO_BLOQUEIO_MS = 10 * 60 * 1000; // 10 minutos em milissegundos

    public boolean isPermitido(String email) {
        long agora = System.currentTimeMillis();

        tempos.putIfAbsent(email, agora);
        tentativas.putIfAbsent(email, 0);

        // Se já passou de 10 minutos, reseta a contagem
        if (agora - tempos.get(email) > TEMPO_BLOQUEIO_MS) {
            tentativas.put(email, 1);
            tempos.put(email, agora);
            return true;
        }

        // Se atingiu o limite, bloqueia
        if (tentativas.get(email) >= MAX_TENTATIVAS) {
            return false;
        }

        // Adiciona mais uma tentativa e permite
        tentativas.put(email, tentativas.get(email) + 1);
        return true;
    }
}
