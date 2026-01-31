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

O **Meus Backlog** evoluiu de uma lista simples para uma aplicação **Fullstack Segura**. A versão atual (V3.0) foca na automação e experiência do usuário, integrando serviços externos para enriquecer o cadastro de itens.

### ✨ Destaques da Versão 3.0 (Atual)
* 🎬 **Busca Automática de Capas:** Integração com a **API da TMDB** para buscar pôsteres oficiais de Filmes e Séries diretamente na tela de cadastro.
* 🎮 **Interface Inteligente:** O sistema detecta o tipo de item (Jogo ou Filme) e adapta a interface, oferecendo busca automática ou inserção manual conforme a necessidade.
* ☁️ **Configuração Profissional:** Proteção de chaves de API utilizando Variáveis de Ambiente.

### 🌟 Destaques das Versões Anteriores
* 🔐 **Autenticação Blindada:** Login e Cadastro com senhas criptografadas (BCrypt).
* 🛡️ **Segurança Avançada:** Proteção contra ataques CSRF e validação rigorosa de senha forte.
* 👤 **Gestão de Perfil Completa:**
    * Alteração de Apelido e Senha.
    * **Zona de Perigo:** Exclusão definitiva de conta (com limpeza em cascata de dados).
* 🔍 **Busca & Filtros:** Pesquisa instantânea e filtragem dinâmica.
* 👁️ **Privacidade (Multi-Tenancy):** Cada usuário tem acesso isolado apenas aos seus próprios itens.

---

## 🛠️ Tecnologias Utilizadas

### Backend (Java Ecosystem)
* **Java 17 & Spring Boot 3:** O coração da aplicação.
* **Spring Security 6:** Gerenciamento de sessões e autenticação.
* **Spring Data JPA:** Abstração para persistência de dados.
* **OpenFeign / RestTemplate:** Para consumo de APIs externas.

### Frontend
* **Thymeleaf:** Renderização de páginas no servidor (SSR).
* **HTML5 & CSS3:** Layout responsivo com Flexbox/Grid e variáveis CSS.
* **JavaScript (ES6+):** Lógica de interface, Fetch API e manipulação do DOM.
* **SweetAlert2:** Alertas modernos e responsivos.

### APIs Externas
* **The Movie Database (TMDB):** Fonte de dados para capas de filmes e séries.

---

## ⚙️ Como Rodar Localmente

### Pré-requisitos
* Java JDK 17+.
* Maven.
* MySQL Server (ou H2 para testes rápidos).
* Uma chave de API gratuita da [TMDB](https://www.themoviedb.org/documentation/api).

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

3.  **Configure a Chave da API (Segurança):**
    Você deve configurar a chave da TMDB como variável de ambiente ou direto no arquivo (não recomendado para produção).
    * **Opção A (Variável de Ambiente - Recomendado):**
      Crie uma variável chamada `TMDB_API_KEY` no seu sistema ou IDE com sua chave.
    * **Opção B (Arquivo):**
      No `application.properties`, altere:
      ```properties
      tmdb.api.key=SUA_CHAVE_AQUI
      ```

4.  **Execute a Aplicação:**
    ```bash
    mvn spring-boot:run
    ```

5.  **Acesse:**
    Abra `http://localhost:8080` no seu navegador.

---

## 🛣️ Roadmap

- [x] V1.0: CRUD Básico de Itens.
- [x] V2.0: Sistema de Login, Segurança e Perfil.
- [x] **V3.0:** Busca Automática de Capas (TMDB) para Filmes e Séries.
- [ ] **V3.1:** Modo Social (Compartilhar lista com amigos).

---

## 🤝 Autor

Desenvolvido com 🤍 e ☕ por **Luiz Augusto**.  
*Estudante de Engenharia de Software & Técnico em Informática*

---
