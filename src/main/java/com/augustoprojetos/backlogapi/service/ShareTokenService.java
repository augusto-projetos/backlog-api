package com.augustoprojetos.backlogapi.service;

import com.augustoprojetos.backlogapi.entity.ShareToken;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.repository.ShareTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ShareTokenService {

    @Autowired
    private ShareTokenRepository shareTokenRepository;

    /**
     * Gera um novo link de compartilhamento.
     * @param user O dono da lista.
     * @param horasValidade Quantas horas o link vai durar.
     * @return O token gerado.
     */
    public ShareToken gerarToken(User user, int horasValidade) {
        // Gera um código aleatório impossível de chutar
        String tokenString = UUID.randomUUID().toString();

        // Calcula a data de expiração (Agora + Horas escolhidas)
        LocalDateTime expiracao = LocalDateTime.now().plusHours(horasValidade);

        // Cria e Salva
        ShareToken token = new ShareToken(tokenString, user, expiracao);
        return shareTokenRepository.save(token);
    }

    /**
     * Busca um token e verifica se ele é válido (não expirou).
     */
    public Optional<ShareToken> obterTokenValido(String tokenString) {
        Optional<ShareToken> tokenOpt = shareTokenRepository.findByToken(tokenString);

        if (tokenOpt.isPresent()) {
            ShareToken token = tokenOpt.get();

            // Se já passou da data de validade, fingimos que não existe
            if (token.isExpirado()) {
                // Apagamos do banco pra limpar sujeira
                shareTokenRepository.delete(token);
                return Optional.empty();
            }

            // Se está vivo, incrementa visualização e salva
            token.setVisualizacoes(token.getVisualizacoes() + 1);
            shareTokenRepository.save(token);

            return Optional.of(token);
        }

        return Optional.empty();
    }

    public List<ShareToken> listarMeusLinks(User user) {
        return shareTokenRepository.findByUser(user);
    }

    public void revogarToken(Long id) {
        shareTokenRepository.deleteById(id);
    }
}