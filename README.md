# 🎮 Meu Backlog Pessoal - API & Frontend

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-blue)
![Docker](https://img.shields.io/badge/Docker-Available-blue)
![Render](https://img.shields.io/badge/Deploy-Render-black)
![License](https://img.shields.io/badge/License-MIT-yellow)

> **Sua coleção, suas regras.** Organize os jogos que você zerou, os filmes que assistiu e as séries que maratonou em um único lugar seguro e moderno.

---

## 🚀 Sobre o Projeto

O **Meus Backlog** evoluiu de uma lista simples para uma aplicação **Fullstack Segura**. A versão atual (V2.0) foca na experiência do usuário e na proteção de dados, implementando um ciclo de vida completo de autenticação e gerenciamento de perfil.

### ✨ Destaques da Versão 2.0

* 🔐 **Autenticação Blindada:** Login e Cadastro com senhas criptografadas (BCrypt).
* 🛡️ **Segurança Avançada:** Proteção contra ataques CSRF e validação rigorosa de senha forte (Regex).
* 👤 **Gestão de Perfil Completa:**
    * Alteração de Apelido.
    * **Troca de Senha Segura:** Exige senha atual e validação de força.
    * **Zona de Perigo:** Exclusão definitiva de conta (com limpeza em cascata de dados).
* 🔍 **Busca & Filtros:** Pesquisa instantânea e filtragem dinâmica (Zerado, Jogando, Backlog).
* 📱 **UX Responsiva:** Design adaptável para Mobile/Desktop com feedbacks visuais elegantes (SweetAlert2).
* 👁️ **Privacidade (Multi-Tenancy):** Cada usuário tem acesso isolado apenas aos seus próprios itens.

---

## 🛠️ Tecnologias Utilizadas

### Backend (Java Ecosystem)
* **Java 17 & Spring Boot 3:** O coração da aplicação.
* **Spring Security 6:** Gerenciamento de sessões, autenticação e proteção de rotas.
* **Spring Data JPA:** Abstração para persistência de dados.
* **Validation API:** Regras de negócio para integridade dos dados.

### Frontend
* **Thymeleaf:** Renderização de páginas no servidor (SSR).
* **HTML5 & CSS3:** Layout responsivo com Flexbox/Grid e variáveis CSS.
* **JavaScript (ES6+):** Lógica de interface, Fetch API para requisições assíncronas e manipulação do DOM.
* **SweetAlert2:** Substituição moderna para os alertas padrões do navegador.

---

## ⚙️ Como Rodar Localmente

### Pré-requisitos
* Java JDK 17+.
* Maven.
* MySQL Server (ou H2 para testes rápidos).

### Passo a Passo

1.  **Clone o repositório:**
    ```bash
    git clone [https://github.com/seu-usuario/meus-backlog.git](https://github.com/seu-usuario/meus-backlog.git)
    ```

2.  **Configure o Banco de Dados:**
    Edite o arquivo `src/main/resources/application.properties`:
    ```properties
    spring.datasource.url=jdbc:mysql://localhost:3306/backlog_db
    spring.datasource.username=seu_usuario
    spring.datasource.password=sua_senha
    spring.jpa.hibernate.ddl-auto=update
    ```

3.  **Execute a Aplicação:**
    ```bash
    mvn spring-boot:run
    ```

4.  **Acesse:**
    Abra `http://localhost:8080` no seu navegador.

---

## 🛣️ Roadmap (Futuro)

- [x] V1.0: CRUD Básico de Itens.
- [x] V2.0: Sistema de Login, Segurança e Perfil.
- [ ] **V3.0:** Integração com APIs Externas (IGDB/TMDB) para buscar capas automaticamente.
- [ ] **V3.1:** Modo Social (Compartilhar lista com amigos).

---

## 🤝 Autor

Desenvolvido com 🤍 e ☕ por **Luiz Augusto**.  
*Estudante de Engenharia de Software & Técnico em Informática*

---
