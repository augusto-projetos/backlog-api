package com.augustoprojetos.backlogapi.service;

import com.augustoprojetos.backlogapi.dto.admin.AdminItemDTO;
import com.augustoprojetos.backlogapi.dto.admin.AdminUserDTO;
import com.augustoprojetos.backlogapi.dto.admin.AdminGlobalStatsDTO;
import com.augustoprojetos.backlogapi.entity.Conquista;
import com.augustoprojetos.backlogapi.entity.Item;
import com.augustoprojetos.backlogapi.entity.User;
import com.augustoprojetos.backlogapi.repository.ConquistaRepository;
import com.augustoprojetos.backlogapi.repository.EmailVerificationTokenRepository;
import com.augustoprojetos.backlogapi.repository.ItemRepository;
import com.augustoprojetos.backlogapi.repository.ShareTokenRepository;
import com.augustoprojetos.backlogapi.repository.UserConquistaRepository;
import com.augustoprojetos.backlogapi.repository.UserRepository;
import com.augustoprojetos.backlogapi.util.NotaScaleUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired private UserRepository userRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private ConquistaRepository conquistaRepository;
    @Autowired private UserConquistaRepository userConquistaRepository;
    @Autowired private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Autowired private ShareTokenRepository shareTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ConquistaService conquistaService;
    @Autowired private AtividadeLogService atividadeLogService;

    @Value("${admin.email}")
    private String adminEmail;

    // --- USUÁRIOS ---

    // Lista todos os usuários verificados (exceto o próprio admin)
    public List<AdminUserDTO> listarUsuariosAtivos() {
        return userRepository.findAll().stream()
                .filter(u -> u.isEmailVerified() && !adminEmail.equalsIgnoreCase(u.getEmail()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Lista usuários aguardando verificação de e-mail
    public List<AdminUserDTO> listarUsuariosPendentes() {
        return userRepository.findAll().stream()
                .filter(u -> !u.isEmailVerified() && !adminEmail.equalsIgnoreCase(u.getEmail()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Lista os e-mails dos usuários ativos (verificados), para disparo em massa de comunicados
    public List<String> listarEmailsUsuariosAtivos() {
        return userRepository.findAll().stream()
                .filter(u -> u.isEmailVerified() && !adminEmail.equalsIgnoreCase(u.getEmail()))
                .map(User::getEmail)
                .collect(Collectors.toList());
    }

    public AdminUserDTO buscarUsuarioPorId(Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        return toDTO(u);
    }

    // Edita nome, @ e email de um usuário
    public void editarUsuario(Long id, String novoLogin, String novoSocial, String novoEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (adminEmail.equalsIgnoreCase(user.getEmail())) {
            throw new RuntimeException("Não é possível editar a conta de administrador por aqui.");
        }

        // Valida o formato do @
        if (novoSocial == null || !UserService.SOCIAL_USERNAME_PATTERN.matcher(novoSocial).matches()) {
            throw new RuntimeException("@ inválido: use apenas letras, números, pontos ou sublinhados.");
        }

        // Valida duplicidade de social, exceto o próprio usuário
        userRepository.findBySocialUsername(novoSocial)
                .filter(found -> !found.getId().equals(id))
                .ifPresent(found -> { throw new RuntimeException("@ já em uso"); });

        userRepository.findByEmail(novoEmail)
                .filter(found -> !found.getId().equals(id))
                .ifPresent(found -> { throw new RuntimeException("E-mail já em uso"); });

        user.setLogin(novoLogin);
        user.setSocialUsername(novoSocial);
        user.setEmail(novoEmail);
        userRepository.save(user);
    }

    // Redefine senha de um usuário sem pedir a antiga
    public void redefinirSenhaUsuario(Long id, String novaSenha) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (adminEmail.equalsIgnoreCase(user.getEmail())) {
            throw new RuntimeException("Não é possível alterar a senha do administrador por aqui.");
        }

        user.setPassword(passwordEncoder.encode(novaSenha));
        userRepository.save(user);
    }

    // Remove usuário e todos os dados relacionados
    @Transactional
    public void deletarUsuario(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (adminEmail.equalsIgnoreCase(user.getEmail())) {
            throw new RuntimeException("Não é possível deletar a conta de administrador.");
        }

        userConquistaRepository.deleteByUser(user);
        itemRepository.deleteByUser(user);
        // Apaga share tokens do usuário (necessário para evitar violação de FK)
        shareTokenRepository.findByUser(user).forEach(shareTokenRepository::delete);
        emailVerificationTokenRepository.findByUser_Id(id).ifPresent(emailVerificationTokenRepository::delete);

        // Remove logs da timeline
        atividadeLogService.deletarLogsByUser(user);

        userRepository.delete(user);
    }

    // Lista itens do backlog de um usuário específico
    public List<Item> listarItensPorUsuario(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        return itemRepository.findByUser(user);
    }


    // Busca item por ID e retorna DTO leve (título, tipo, dono) — usado pelo audit log antes de deletar
    public AdminItemDTO buscarItemPorId(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item não encontrado"));
        AdminItemDTO dto = new AdminItemDTO();
        dto.setId(item.getId());
        dto.setTitulo(item.getTitulo());
        dto.setTipo(item.getTipo());
        dto.setStatus(item.getStatus());
        dto.setUserLogin(item.getUser() != null ? item.getUser().getLogin() : "(desconhecido)");
        return dto;
    }

    // Edita status, nota e resenha de qualquer item (acesso admin)
    public void editarItem(Long itemId, String status, Double nota, String resenha) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item não encontrado"));
        if (status != null && !status.isBlank()) item.setStatus(status);
        if (nota != null) item.setNota(nota);
        item.setResenha(resenha);
        itemRepository.save(item);
    }

    // Remove um item específico do backlog de qualquer usuário
    @Transactional
    public void deletarItem(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item não encontrado"));
        itemRepository.delete(item);
    }

    // --- ESTATÍSTICAS GLOBAIS ---

    public AdminGlobalStatsDTO calcularEstatisticasGlobais() {
        AdminGlobalStatsDTO stats = new AdminGlobalStatsDTO();

        long totalUsuarios = userRepository.findAll().stream()
                .filter(u -> !adminEmail.equalsIgnoreCase(u.getEmail()))
                .filter(User::isEmailVerified)
                .count();

        long totalPendentes = userRepository.findAll().stream()
                .filter(u -> !adminEmail.equalsIgnoreCase(u.getEmail()))
                .filter(u -> !u.isEmailVerified())
                .count();

        List<Item> todosItens = itemRepository.findAll();

        long totalFilmes = todosItens.stream().filter(i -> "Filme".equalsIgnoreCase(i.getTipo())).count();
        long totalSeries = todosItens.stream().filter(i -> "Série".equalsIgnoreCase(i.getTipo())).count();
        long totalJogos  = todosItens.stream().filter(i -> "Jogo".equalsIgnoreCase(i.getTipo())).count();

        long totalAssistidos = todosItens.stream()
                .filter(i -> i.getStatus() != null)
                .filter(i -> i.getStatus().contains("Assistido") || i.getStatus().contains("Zerado")).count();
        long totalAssistindo = todosItens.stream()
                .filter(i -> i.getStatus() != null)
                .filter(i -> i.getStatus().contains("Assistindo") || i.getStatus().contains("Jogando")).count();
        long totalBacklog = todosItens.stream()
                .filter(i -> i.getStatus() != null)
                .filter(i -> i.getStatus().contains("Backlog")).count();
        long totalDropados = todosItens.stream()
                .filter(i -> i.getStatus() != null)
                .filter(i -> i.getStatus().contains("Dropado")).count();

        // Distribuição de notas global
        Map<Double, Long> contagemPorNota = todosItens.stream()
                .filter(i -> i.getNota() != null && i.getNota() > 0)
                .collect(Collectors.groupingBy(Item::getNota, Collectors.counting()));
        Map<String, Long> distribNotas = NotaScaleUtil.apenasComItens(contagemPorNota);

        stats.setTotalUsuarios(totalUsuarios);
        stats.setTotalPendentes(totalPendentes);
        stats.setTotalFilmes(totalFilmes);
        stats.setTotalSeries(totalSeries);
        stats.setTotalJogos(totalJogos);
        stats.setTotalItens(todosItens.size());
        stats.setTotalAssistidos(totalAssistidos);
        stats.setTotalAssistindo(totalAssistindo);
        stats.setTotalBacklog(totalBacklog);
        stats.setTotalDropados(totalDropados);
        stats.setDistribNotas(distribNotas);

        // Tempo gasto global (Filmes e Jogos informados manualmente pelos
        // usuários, sempre em minutos)
        long minutosFilmes = todosItens.stream()
                .filter(i -> "Filme".equalsIgnoreCase(i.getTipo()) && i.getDuracaoMinutos() != null)
                .mapToLong(i -> i.getDuracaoMinutos())
                .sum();
        long minutosJogos = todosItens.stream()
                .filter(i -> "Jogo".equalsIgnoreCase(i.getTipo()) && i.getMinutosJogados() != null)
                .mapToLong(i -> i.getMinutosJogados())
                .sum();

        stats.setMinutosFilmes(minutosFilmes);
        stats.setMinutosJogos(minutosJogos);

        return stats;
    }

    // --- CONQUISTAS ---

    public List<Conquista> listarConquistas() {
        return conquistaRepository.findAll();
    }

    public Conquista buscarConquistaPorId(Long id) {
        return conquistaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conquista não encontrada"));
    }

    public void criarConquista(Conquista conquista) {
        if (conquistaRepository.findByChave(conquista.getChave()).isPresent()) {
            throw new RuntimeException("Chave já existe: " + conquista.getChave());
        }
        conquistaRepository.save(conquista);
    }

    public void editarConquista(Long id, Conquista dados) {
        Conquista existente = buscarConquistaPorId(id);
        // Valida chave única exceto a própria conquista
        conquistaRepository.findByChave(dados.getChave())
                .filter(found -> !found.getId().equals(id))
                .ifPresent(found -> { throw new RuntimeException("Chave já em uso"); });

        existente.setChave(dados.getChave());
        existente.setNome(dados.getNome());
        existente.setDescricao(dados.getDescricao());
        existente.setIcone(dados.getIcone());
        existente.setXp(dados.getXp());
        existente.setCriterioTipo(dados.getCriterioTipo());
        existente.setCriterioValor(dados.getCriterioValor());
        conquistaRepository.save(existente);
    }

    // Concede uma conquista a um usuário (ação administrativa).
    // Retorna true se concedida, false se o usuário já a possuía.
    @Transactional
    public boolean concederConquistaParaUsuario(Long userId, Long conquistaId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        boolean concedida = conquistaService.concederConquistaAdmin(user, conquistaId);

        // Registra na timeline do usuário, assim como as conquistas automáticas
        if (concedida) {
            conquistaRepository.findById(conquistaId).ifPresent(c ->
                    atividadeLogService.registrarConquistaDesbloqueada(user, c.getNome(), c.getIcone()));
        }

        return concedida;
    }

    // Revoga uma conquista de um usuário (ação administrativa).
    // Retorna o XP deduzido, ou -1 se o usuário não possuía a conquista.
    @Transactional
    public int revogarConquistaDoUsuario(Long userId, Long conquistaId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        return conquistaService.revogarConquistaAdmin(user, conquistaId);
    }

    // Lista as conquistas que um usuário específico já possui.
    public List<com.augustoprojetos.backlogapi.entity.UserConquista> listarConquistasDoUsuario(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        return userConquistaRepository.findByUser(user);
    }

    @Transactional
    public void deletarConquista(Long id) {
        Conquista c = buscarConquistaPorId(id);
        userConquistaRepository.deleteByConquista(c);
        conquistaRepository.delete(c);
    }

    // --- HELPERS ---

    private AdminUserDTO toDTO(User u) {
        AdminUserDTO dto = new AdminUserDTO();
        dto.setId(u.getId());
        dto.setLogin(u.getLogin());
        dto.setEmail(u.getEmail());
        dto.setSocialUsername(u.getSocialUsername());
        dto.setEmailVerified(u.isEmailVerified());
        dto.setPublic(u.isPublic());
        dto.setTotalItens(itemRepository.findByUser(u).size());
        return dto;
    }
}
