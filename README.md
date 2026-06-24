# 🎮 Meu Backlog Pessoal - API & Frontend

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3-green)
![Spring Security](https://img.shields.io/badge/Spring_Security-6-6db33f)
![Chart.js](https://img.shields.io/badge/Frontend-Chart.js-FF6384)
![Render](https://img.shields.io/badge/Deploy-Render-black)
![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-%23FE5196?logo=conventionalcommits&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-yellow)
![Brevo](https://img.shields.io/badge/API-Brevo_Email-009286)
![TMDB](https://img.shields.io/badge/API-TMDB-01b4e4)

> **Sua coleção, suas regras.** Organize os jogos que você zerou, os filmes que assistiu e as séries que maratonou em um único lugar seguro e moderno.

---

## 🚀 Sobre o Projeto

O **Meus Backlog** é uma aplicação **Fullstack** robusta que simula um ambiente de produção real. A versão atual (**V6.0**) marca a maior evolução do sistema, transformando-o de um gerenciador pessoal em uma verdadeira **Rede Social de Colecionadores**, com perfis públicos, sistema de buscas e autenticação blindada com verificação de e-mail.

### ✨ Destaques da Versão 6.0 (A Atualização Social)
* 🌐 **Perfis Públicos & Identidade:** Todo usuário agora possui um `@username` único (`socialUsername`). A plataforma gera dinamicamente uma URL de perfil público (`/u/seu_arroba`) no formato Read-Only para exibição do acervo e métricas para amigos.
* 🔍 **Busca Social Inteligente:** Nova barra de pesquisa no cabeçalho com requisições assíncronas em tempo real. Utiliza a técnica de *Debounce* no JavaScript para poupar requisições ao banco de dados e conta com uma UI adaptativa (botão circular flutuante) desenhada para uma experiência Mobile-First.
* 📧 **Autenticação Avançada & E-mail:** Fluxo de cadastro seguro com geração de *tokens* temporários (`EmailVerificationToken`) e envio de e-mails de confirmação utilizando a API do **Brevo**.
* 🛡️ **Defesa Anti-Spam:** Implementação de `RateLimitService` para proteger os endpoints de registro e reenvio de e-mails contra abusos e requisições em massa.

### 🌟 Funcionalidades Consolidadas
* 📄 **Exportação de Relatórios (V5.0):** Geração de relatórios gerenciais em PDF (layout para impressão) e Excel (planilha para análise), com processamento 100% em memória RAM via `ByteArrayOutputStream`.
* 📊 **Dashboard de BI:** Gráficos interativos (Chart.js) com distribuição de acervo, status de progresso e histograma de notas.
* 🎬 **Integração TMDB:** Busca automática de capas, sinopses e metadados de filmes/séries.
* 🌐 **Modo Share:** Links públicos temporários para compartilhar listas específicas.
* 🔍 **Filtros Avançados:** "Gaveta" de filtros com ordenação por nota, tipo e status.

---

## 🛠️ Arquitetura & Tecnologias

### Backend (Java Ecosystem)
* **Java 17 & Spring Boot 3:** API REST e arquitetura MVC.
* **Spring Security 6:** Controle de sessão, criptografia (BCrypt), autorização de rotas e proteção CSRF.
* **JPA/Hibernate:** Consultas otimizadas, uso extensivo de `Optional` e DTOs (Records).
* **Apache POI & OpenPDF:** Manipulação e geração em tempo real de arquivos Office e PDFs.
* **Sentry SDK:** Monitoramento proativo de erros em produção.

### Frontend
* **Thymeleaf:** Renderização Server-Side (SSR) acoplada ao backend.
* **Vanilla JavaScript:** Lógica de interface com chamadas assíncronas (`fetch API`), debounce para buscas e manipulação de DOM baseada em eventos.
* **CSS3 (Modern UI):** Variáveis nativas (Dark/Light Mode) e Flexbox/Grid para designs altamente responsivos.
* **Chart.js:** Biblioteca Canvas para renderização de dados estatísticos.

### APIs Externas
* **TMDB (The Movie Database):** Consumo de REST API para metadados de mídia.
* **Brevo API:** Serviço de mensageria para validação de contas via SMTP.

---

## ⚙️ Como Rodar Localmente

### Pré-requisitos
* Java JDK 17+.
* Maven.
* MySQL Server.
* Chaves de API externas (TMDB e Brevo).
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
    * `TMDB_API_KEY`: Para buscar capas.
    * `BREVO_API_KEY`: Para o envio de e-mails de verificação de conta.
    * `SENTRY_DSN`: Monitoramento de logs (Opcional).

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
- [x] V3.1: Modo Social e Monitoramento (Sentry/Umami).
- [x] V4.0: Dashboard de Estatísticas (Chart.js + DTOs).
- [x] V5.0: Exportação de Relatórios (Gerar PDF/Excel da coleção).
- [x] V6.0: Rede Social (Busca dinâmica, Perfis com @ e Verificação de E-mail).
- [ ] **V7.0: Sistema com IA e Conquistas**

---

## 🤝 Autor

Desenvolvido com 🤍 e ☕ por **Luiz Augusto**.  
*Técnico em Informática*  
*Engenharia de Software | Graduando na Universidade Federal de Lavras (UFLA)*

---