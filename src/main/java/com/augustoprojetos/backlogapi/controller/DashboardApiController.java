package com.augustoprojetos.backlogapi.controller;

import com.augustoprojetos.backlogapi.dto.DashboardStatsDTO;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.repository.ItemRepository;
import com.augustoprojetos.backlogapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<DashboardStatsDTO> getEstatisticas() {
        // 1. Pega o usuário logado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String login = auth.getName();

        User user = (User) userRepository.findByLogin(login);

        // 2. Cria o DTO vazio
        DashboardStatsDTO stats = new DashboardStatsDTO();

        if (user == null) return ResponseEntity.notFound().build();

        // 3. Processa TIPOS
        List<Object[]> listaTipos = itemRepository.countItensPorTipo(user.getId());
        for (Object[] obj : listaTipos) {
            String tipo = (String) obj[0];
            Long qtd = (Long) obj[1];

            if ("Jogo".equalsIgnoreCase(tipo)) stats.setTotalJogos(qtd);
            else if ("Filme".equalsIgnoreCase(tipo)) stats.setTotalFilmes(qtd);
            else if ("Série".equalsIgnoreCase(tipo)) stats.setTotalSeries(qtd);
        }

        // 4. Processa STATUS
        List<Object[]> listaStatus = itemRepository.countItensPorStatus(user.getId());
        for (Object[] obj : listaStatus) {
            String status = (String) obj[0];
            Long qtd = (Long) obj[1];

            if (status.contains("Zerado") || status.contains("Assistido")) {
                stats.setTotalZerados(stats.getTotalZerados() + qtd);
            } else if (status.contains("Jogando") || status.contains("Assistindo")) {
                stats.setTotalJogando(stats.getTotalJogando() + qtd);
            } else if (status.contains("Backlog")) {
                stats.setTotalBacklog(qtd);
            } else if (status.contains("Dropado")) {
                stats.setTotalDropados(qtd);
            }
        }

        return ResponseEntity.ok(stats);
    }
}