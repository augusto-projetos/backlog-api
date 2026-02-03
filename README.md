# 🎮 Meu Backlog Pessoal - API & Frontend

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3-green)
![Spring Security](https://img.shields.io/badge/Spring_Security-6-6db33f)
![Render](https://img.shields.io/badge/Deploy-Render-black)
![License](https://img.shields.io/badge/License-MIT-yellow)
![TMDB](https://img.shields.io/badge/API-TMDB-01b4e4)

> **Sua coleção, suas regras.** Organize os jogos que você zerou, os filmes que assistiu e as séries que maratonou em um único lugar seguro e moderno.

---

## 🚀 Sobre o Projeto

O **Meus Backlog** é uma aplicação **Fullstack Segura** que simula um ambiente de produção real. A versão atual (V3.1) transforma o projeto numa plataforma social e monitorada profissionalmente.

### ✨ Destaques da Versão 3.1 (Atual)
* 🌐 **Modo Social (Compartilhamento):** Gere links públicos temporários (24h, 3 dias ou 7 dias) para mostrar a sua coleção aos amigos sem que eles precisem logar. Inclui painel de gerenciamento para revogar acessos.
* 📊 **Observabilidade Profissional:** Integração com **Sentry** para monitoramento de erros em tempo real e **Umami** para analytics focado em privacidade.
* 📉 **Filtros Avançados:** Nova ordenação por "Maior Nota" e "Menor Nota", além da busca textual e por status.
* 📱 **UX Mobile Aprimorada:** Interface 100% responsiva, com botões adaptáveis e melhorias na usabilidade em telas pequenas.

### 🌟 Funcionalidades Consolidadas
* 🎬 **Busca Automática de Capas:** Integração com a **API da TMDB** para buscar pôsteres oficiais.
* 🔐 **Segurança de Ponta:** Login blindado (BCrypt), proteção CSRF e rotas autenticadas.
* 👤 **Gestão de Perfil:** Alteração de dados sensíveis e "Zona de Perigo" (Exclusão de conta).
* 👁️ **Privacidade (Multi-Tenancy):** Dados isolados por usuário.

---

## 🛠️ Tecnologias Utilizadas

### Backend (Java Ecosystem)
* **Java 17 & Spring Boot 3:** Core da aplicação.
* **Spring Security 6:** Autenticação e Autorização (incluindo rotas públicas via Token UUID).
* **Sentry SDK:** Monitoramento de erros e performance.
* **Spring Data JPA & Hibernate:** Persistência de dados.

### Frontend
* **Thymeleaf:** Renderização dinâmica (SSR).
* **HTML5, CSS3 & JavaScript:** Layout responsivo e interatividade sem frameworks pesados.
* **SweetAlert2:** Alertas modais elegantes.
* **Umami Analytics:** Métricas de acesso respeitando a privacidade.

### APIs Externas
* **The Movie Database (TMDB):** Metadados de filmes e séries.

---

## ⚙️ Como Rodar Localmente

### Pré-requisitos
* Java JDK 17+.
* Maven.
* MySQL Server.
* Chave da API [TMDB](https://www.themoviedb.org/).
* DSN do [Sentry](https://sentry.io/) (Opcional).

### Passo a Passo

1.  **Clone o repositório:**
    ```bash
    git clone https://github.com/seu-usuario/meus-backlog.git
    ```

2.  **Configure o Banco de Dados:**
    Edite o `src/main/resources/application.properties`:
    ```properties
    spring.datasource.url=jdbc:mysql://localhost:3306/backlog_db
    spring.datasource.username=seu_usuario
    spring.datasource.password=sua_senha
    ```

3.  **Variáveis de Ambiente (Recomendado):**
    Configure as chaves no seu sistema ou IDE:
    * `TMDB_API_KEY`: Sua chave da TMDB.
    * `SENTRY_DSN`: Seu link do projeto Sentry (se for usar).

4.  **Execute a Aplicação:**
    ```bash
    mvn spring-boot:run
    ```

5.  **Acesse:**
    Abra `http://localhost:8080` no seu navegador.

---

## 🛣️ Roadmap & Futuro

- [x] V1.0: CRUD Básico.
- [x] V2.0: Login, Segurança e Perfil.
- [x] V3.0: Integração TMDB e Capas.
- [x] **V3.1: Modo Social e Monitoramento (Sentry/Umami).**
- [ ] **V4.0: Dashboard de Estatísticas** (Gráficos visuais de quantos filmes vs jogos, nota média, gêneros favoritos).
- [ ] **V4.1: Gamificação** (Conquistas/Badges por quantidade de itens cadastrados).

---

## 🤝 Autor

Desenvolvido com 🤍 e ☕ por **Luiz Augusto**.  
*Estudante de Engenharia de Software & Técnico em Informática*

---
