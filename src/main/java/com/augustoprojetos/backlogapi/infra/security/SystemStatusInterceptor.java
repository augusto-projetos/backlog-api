package com.augustoprojetos.backlogapi.infra.security;

import com.augustoprojetos.backlogapi.service.SystemConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class SystemStatusInterceptor implements HandlerInterceptor {

    @Autowired
    private SystemConfigService configService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 1. Recursos estáticos e essenciais sempre passam direto (sem verificação)
        if (isPublicResource(uri)) return true;

        // 2. Se o usuário JÁ ESTÁ autenticado como ADMIN, ele tem passe livre total
        if (isAdmin()) return true;

        // 3. Sistema Bloqueado: bloqueia todas as rotas, exceto login e home
        if (configService.isBloqueado()) {
            boolean rotaLoginOuEssencial = uri.equals("/")
                    || uri.equals("/login")
                    || uri.startsWith("/logout")
                    || uri.startsWith("/health");

            if (!rotaLoginOuEssencial) {
                response.sendRedirect("/");
                return false;
            }
        }

        // 4. Modo Somente-Leitura: bloqueia alterações, mas permite leitura e navegação
        if (configService.isModoReadonly()) {
            if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
                // Se for requisição para o próprio admin alterar configurações no painel, permite!
                if (uri.startsWith("/admin/sistema")) {
                    return true;
                }

                // Se for uma requisição de API/AJAX do usuário comum tentando mexer no backlog
                if (uri.startsWith("/api") || uri.startsWith("/admin/api") || request.getHeader("X-Requested-With") != null || "application/json".equalsIgnoreCase(request.getContentType())) {
                    // Retorna status 423 (Locked)
                    response.setStatus(423);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"readonly\":true,\"erro\":\"O sistema está temporariamente em Modo Somente-Leitura. Suas alterações não foram salvas.\"}");
                } else {
                    // Se for uma requisição de página normal, joga de volta pra home
                    response.sendRedirect("/home");
                }
                return false;
            }
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView mav) {
        if (mav == null) return;

        // Garante que o painel de admin não injete os banners de aviso para o próprio admin
        if (isAdmin()) return;

        // Injeta flags de status em tempo real em todas as views do usuário comum
        mav.addObject("sistemaManutencao",  configService.isModoManutencao());
        mav.addObject("sistemaInstavel",    configService.isModoInstavel());
        mav.addObject("sistemaBloqueado",   configService.isBloqueado());
        mav.addObject("sistemaReadonly",    configService.isModoReadonly());

        String novidades = configService.getNovidadesTexto();
        if (novidades != null && !novidades.isBlank()) {
            mav.addObject("sistemaNovidades", novidades);
        }
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));
    }

    // Método auxiliar para garantir rotas públicas de login livres
    private boolean isPublicResource(String uri) {
        return uri.startsWith("/css/")
                || uri.startsWith("/js/")
                || uri.startsWith("/img/")
                || uri.startsWith("/favicon.ico")
                || uri.startsWith("/health");
    }
}
