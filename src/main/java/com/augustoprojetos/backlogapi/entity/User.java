package com.augustoprojetos.backlogapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor // Lombok: Cria construtor vazio
@AllArgsConstructor // Lombok: Cria construtor com tudo
// implements UserDetails
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O login é obrigatório")
    private String login;

    @NotBlank(message = "A senha é obrigatória")
    private String password;

    @NotBlank
    @Email(message = "O formato do email é inválido")
    @Column(unique = true)
    private String email;

    // --- Métodos obrigatórios do UserDetails ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Por enquanto, todo mundo é USER comum.
        // No futuro, teremos ADMIN aqui.
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getUsername() {
        return login; // Avisa pro Spring que nosso "username" é o campo "login"
    }

    @Override
    public String getPassword() {
        return password; // Avisa onde está a senha
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // A conta nunca expira
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // A conta nunca é bloqueada
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // A senha nunca vence
    }

    @Override
    public boolean isEnabled() {
        return true; // O usuário está sempre ativo
    }

    // Método explícito para o Thymeleaf ler o nome
    public String getLogin() {
        return this.login;
    }
}