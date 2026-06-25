package com.augustoprojetos.backlogapi.infra.security;

import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.entity.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Habilita @PreAuthorize nos controllers
public class SecurityConfigurations {

    @Value("${admin.email}")
    private String adminEmail;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        // PÚBLICO
                        .requestMatchers("/", "/login", "/register", "/auth/**", "/css/**", "/js/**", "/img/**", "/share/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/conquistas/u/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/recuperar-senha", "/resetar-senha", "/reenviar-email").permitAll()
                        .requestMatchers(HttpMethod.POST, "/recuperar-senha", "/resetar-senha").permitAll()
                        // PAINEL ADM: SOMENTE ROLE_ADMIN
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // QUALQUER OUTRA ROTA: AUTENTICADO
                        .anyRequest().authenticated()
                )

                // Formulário de login
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(adminAwareSuccessHandler()) // Handler inteligente de redirect
                        .permitAll()
                )

                // Logout: invalida sessão, deleta cookie e redireciona para /
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )

                // Se alguém sem ROLE_ADMIN tentar acessar /admin, vai para /home
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/home")
                )
                .build();
    }

    /*
     * Após login bem-sucedido:
     * - Se o email for o do admin E o usuário tiver ROLE_ADMIN → /admin
     * - Qualquer outro → /home
     */
    @Bean
    public AuthenticationSuccessHandler adminAwareSuccessHandler() {
        return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User user) {
                if (adminEmail.equalsIgnoreCase(user.getEmail()) && user.getRole() == UserRole.ADMIN) {
                    response.sendRedirect("/admin");
                    return;
                }
            }
            response.sendRedirect("/home");
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}