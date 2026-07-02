package com.augustoprojetos.backlogapi.controller;

import com.augustoprojetos.backlogapi.dto.UpdateProfileDTO;
import com.augustoprojetos.backlogapi.dto.UserProfileDTO;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.entity.Item;
import com.augustoprojetos.backlogapi.repository.ItemRepository;
import com.augustoprojetos.backlogapi.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    // 1. ATUALIZAR O PRÓPRIO PERFIL (O utilizador escolhe o seu @ e a visibilidade)
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody @Valid UpdateProfileDTO data) {
        // Pega o utilizador que está autenticado neste momento
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();

        // Verifica se o @ escolhido já existe e pertence a outra pessoa
        Optional<User> existingUser = userRepository.findBySocialUsername(data.socialUsername());
        if (existingUser.isPresent() && !existingUser.get().getId().equals(currentUser.getId())) {
            return ResponseEntity.badRequest().body("Este @ já está sendo utilizado por outra pessoa.");
        }

        // Atualiza e guarda na base de dados
        currentUser.setSocialUsername(data.socialUsername());
        currentUser.setPublic(data.isPublic());
        userRepository.save(currentUser);

        return ResponseEntity.ok("Perfil atualizado com sucesso!");
    }

    // 2. PESQUISAR UTILIZADORES PÚBLICOS
    @GetMapping("/search")
    public ResponseEntity<List<UserProfileDTO>> searchPublicUsers(@RequestParam("q") String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        // Procura na base de dados (ignorando maiúsculas/minúsculas)
        List<User> users = userRepository.findBySocialUsernameContainingIgnoreCaseAndIsPublicTrue(query.trim());

        // Converte a entidade User para o DTO (escondendo emails, passwords, etc)
        List<UserProfileDTO> response = users.stream()
                .map(u -> new UserProfileDTO(u.getId(), u.getSocialUsername()))
                .toList();

        return ResponseEntity.ok(response);
    }

    // 3. BUSCAR UM PERFIL ESPECÍFICO
    @GetMapping("/{socialUsername}")
    public ResponseEntity<UserProfileDTO> getUserProfile(@PathVariable String socialUsername) {
        Optional<User> userOpt = userRepository.findBySocialUsername(socialUsername);

        if (userOpt.isEmpty() || !userOpt.get().isPublic()) {
            // Retorna 404 se não existir OU se for privado
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(new UserProfileDTO(userOpt.get().getId(), userOpt.get().getSocialUsername()));
    }

    // 4. TRAZER O BACKLOG DE UM PERFIL PÚBLICO
    @GetMapping("/{socialUsername}/backlog")
    public ResponseEntity<?> getPublicUserBacklog(@PathVariable String socialUsername) {
        // 1. Busca o usuário pelo @
        Optional<User> userOpt = userRepository.findBySocialUsername(socialUsername);

        // 2. Trava de segurança: Se não existir ou se for privado, devolve Erro 404
        if (userOpt.isEmpty() || !userOpt.get().isPublic()) {
            return ResponseEntity.notFound().build();
        }

        User targetUser = userOpt.get();

        // 3. Busca a coleção de itens usando o usuário que acabamos de encontrar
        List<Item> backlog = itemRepository.findByUser(targetUser);

        // 4. Devolve a lista de itens pronta para o Frontend desenhar
        return ResponseEntity.ok(backlog);
    }
}
