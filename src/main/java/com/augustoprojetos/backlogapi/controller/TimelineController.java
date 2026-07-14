package com.augustoprojetos.backlogapi.controller;

import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.service.AtividadeLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/timeline")
public class TimelineController {

    @Autowired
    private AtividadeLogService atividadeLogService;

    @GetMapping
    public String timeline(@AuthenticationPrincipal User userLogado, Model model) {
        model.addAttribute("nomeUsuario", userLogado.getLogin());
        var logs = atividadeLogService.buscarTimelineDoUsuario(userLogado);
        model.addAttribute("logs", logs);
        model.addAttribute("grupos", atividadeLogService.agruparParaTimeline(logs));
        return "timeline";
    }
}
