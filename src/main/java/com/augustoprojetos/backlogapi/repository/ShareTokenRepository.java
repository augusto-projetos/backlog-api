package com.augustoprojetos.backlogapi.repository;

import com.augustoprojetos.backlogapi.entity.ShareToken;
import com.augustoprojetos.backlogapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ShareTokenRepository extends JpaRepository<ShareToken, Long> {

    // Para quando o amigo acessar o link: "Ei banco, existe esse token?"
    Optional<ShareToken> findByToken(String token);

    // Para mostrar no perfil do usuário: "Quais links eu criei?"
    List<ShareToken> findByUser(User user);

    // Para limpar tokens expirados
    void deleteByToken(String token);
}