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

    // --- CADASTRAR COM SEGURANÇA ---
    public void cadastrarUsuario(User user) {
        // 1. Valida antes de tudo
        assert user.getPassword() != null;
        validarRequisitosSenha(user.getPassword());

        // 2. Criptografa
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 3. Salva
        userRepository.save(user);
    }

    // Trocar Senha
    public void trocarSenha(User user, String senhaAntiga, String novaSenha) {
        if (!passwordEncoder.matches(senhaAntiga, user.getPassword())) {
            throw new IllegalArgumentException("Senha atual incorreta.");
        }

        if (passwordEncoder.matches(novaSenha, user.getPassword())) {
            throw new IllegalArgumentException("A nova senha não pode ser igual à atual.");
        }

        // Valida a nova senha
        validarRequisitosSenha(novaSenha);

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

    // Metodo Auxiliar de Validação
    private void validarRequisitosSenha(String senha) {
        if (senha == null || senha.length() < 8) {
            throw new IllegalArgumentException("A senha deve ter no mínimo 8 caracteres.");
        }

        boolean temMaiuscula = false;
        boolean temMinuscula = false;
        boolean temNumero = false;
        boolean temEspecial = false;

        // Lista de caracteres especiais permitidos (igual ao seu regex antigo)
        String especiais = "!@#$%^&*(),.?\":{}|<>";

        // Loop simples: verifica caractere por caractere
        for (char c : senha.toCharArray()) {
            if (Character.isUpperCase(c)) {
                temMaiuscula = true;
            } else if (Character.isLowerCase(c)) {
                temMinuscula = true;
            } else if (Character.isDigit(c)) {
                temNumero = true;
            } else if (especiais.indexOf(c) >= 0) {
                // Se o caractere atual existe na nossa lista de especiais
                temEspecial = true;
            }
        }

        if (!temMaiuscula || !temMinuscula || !temNumero || !temEspecial) {
            throw new IllegalArgumentException("A nova senha não atende aos requisitos de segurança.");
        }
    }
}