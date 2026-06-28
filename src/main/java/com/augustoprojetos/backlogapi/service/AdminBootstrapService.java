package com.augustoprojetos.backlogapi.service;

import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.entity.UserRole;
import com.augustoprojetos.backlogapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminBootstrapService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureAdminExists() {
        var existing = userRepository.findByEmail(adminEmail);

        if (existing.isEmpty()) {
            // Cria a conta do admin
            User admin = new User();
            admin.setLogin("Admin");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setSocialUsername("admin_backlog");
            admin.setEmailVerified(true);
            admin.setPublic(false);
            admin.setRole(UserRole.ADMIN);
            userRepository.save(admin);
            System.out.println("[ADM] Conta de administrador criada com sucesso.");
        } else {
            User admin = existing.get();
            // Garante que sempre tem ROLE_ADMIN (mesmo que alguém mexa direto no banco)
            if (admin.getRole() != UserRole.ADMIN) {
                admin.setRole(UserRole.ADMIN);
                admin.setEmailVerified(true);
                userRepository.save(admin);
                System.out.println("[ADM] Role de administrador restaurada.");
            }
        }
    }
}
