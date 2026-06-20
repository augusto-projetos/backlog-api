package com.augustoprojetos.backlogapi.repository;

import com.augustoprojetos.backlogapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    UserDetails findByLogin(String login);
    UserDetails findByEmail(String email);
    Optional<User> findBySocialUsername(String socialUsername);
    // Busca utilizadores onde o @ contenha o texto pesquisado E o perfil seja público
    List<User> findBySocialUsernameContainingIgnoreCaseAndIsPublicTrue(String socialUsername);
}