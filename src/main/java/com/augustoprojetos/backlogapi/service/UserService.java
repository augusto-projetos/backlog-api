package com.augustoprojetos.backlogapi.service;

import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.repository.ItemRepository;
import com.augustoprojetos.backlogapi.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Trocar Senha
    public void trocarSenha(User user, String senhaAntiga, String novaSenha) {
        // Verifica se a senha antiga está certa
        if (!passwordEncoder.matches(senhaAntiga, user.getPassword())) {
            throw new IllegalArgumentException("Senha atual incorreta.");
        }

        // Verifica se a nova senha é IGUAL à antiga
        if (passwordEncoder.matches(novaSenha, user.getPassword())) {
            throw new IllegalArgumentException("A nova senha não pode ser igual à atual.");
        }

        // Se passar, salva
        user.setPassword(passwordEncoder.encode(novaSenha));
        userRepository.save(user);
    }

    // Atualizar Apelido
    public void atualizarApelido(User user, String novoApelido) {
        user.setLogin(novoApelido);
        userRepository.save(user);
    }

    // Deletar Conta (e tudo relacionado a ela)
    @Transactional // Garante que apaga tudo ou nada
    public void deletarConta(User user) {
        // Primeiro apaga os itens desse usuário
        itemRepository.deleteByUser(user);
        // Depois apaga o usuário
        userRepository.delete(user);
    }
}