package com.augustoprojetos.backlogapi.infra.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfigurations {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        // LIBERA O ACESSO PÚBLICO PARA:
                        .requestMatchers("/", "/login", "/register", "/auth/**", "/css/**", "/js/**", "/images/**").permitAll()
                        // QUALQUER OUTRA COISA PRECISA DE SENHA:
                        .anyRequest().authenticated()
                )
                // CONFIGURA O FORMULÁRIO DE LOGIN
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/home", true) // Se logar com sucesso, vai pra lista
                        .permitAll()
                )
                // LOGOUT
                .logout(logout -> logout
                        .logoutUrl("/logout") // O form do HTML chama aqui
                        .logoutSuccessUrl("/") // Depois de sair, vai pra capa
                        .invalidateHttpSession(true) // Destrói a sessão
                        .deleteCookies("JSESSIONID") // Apaga o rastro
                )
                .build();
    }

    // ENSINA O SPRING A CRIPTOGRAFAR SENHAS
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}