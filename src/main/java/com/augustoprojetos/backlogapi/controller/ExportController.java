package com.augustoprojetos.backlogapi.controller;

import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.repository.UserRepository;
import com.augustoprojetos.backlogapi.service.ExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    @Autowired
    private ExportService exportService;

    @Autowired
    private UserRepository userRepository;

    // --- 1. DOWNLOAD EXCEL ---
    @GetMapping("/excel")
    public ResponseEntity<InputStreamResource> exportarExcel() throws IOException {
        User user = getUsuarioLogado();

        ByteArrayInputStream in = exportService.gerarExcel(user);

        // Cabeçalhos HTTP para forçar o download
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=meus-backlog.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }

    // --- 2. DOWNLOAD PDF ---
    @GetMapping("/pdf")
    public ResponseEntity<InputStreamResource> exportarPDF() {
        User user = getUsuarioLogado();

        ByteArrayInputStream in = exportService.gerarPDF(user);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=meus-backlog.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(in));
    }

    // Metodo auxiliar para não repetir código
    private User getUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Ajuste aqui se seu UserRepository busca por 'login' ou 'email'
        return (User) userRepository.findByLogin(auth.getName());
    }
}