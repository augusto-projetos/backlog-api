package com.augustoprojetos.backlogapi.service;

import com.augustoprojetos.backlogapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService implements UserDetailsService {

    @Autowired
    private UserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // O Optional já abre e pega o usuário. Se estiver vazio, ele mesmo lança o erro!
        return repository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Email não encontrado!"));
    }
}