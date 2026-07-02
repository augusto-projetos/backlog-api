package com.augustoprojetos.backlogapi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthCheckController {

    @GetMapping
    public String check() {
        // Este endpoint não injeta Repository ou Service.
        // Assim, o Spring processa a requisição sem precisar do banco.
        return "Online";
    }
}
