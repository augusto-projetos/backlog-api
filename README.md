# 🎮 Meu Backlog Pessoal - API & Frontend

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3-green)
![Spring Security](https://img.shields.io/badge/Spring_Security-6-6db33f)
![Chart.js](https://img.shields.io/badge/Frontend-Chart.js-FF6384)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Neon_Cloud-blue)
![Groq](https://img.shields.io/badge/API-Groq_Cloud-orange)
![Render](https://img.shields.io/badge/Deploy-Render-black)
![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-%23FE5196?logo=conventionalcommits&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-yellow)
![Brevo](https://img.shields.io/badge/API-Brevo_Email-009286)
![TMDB](https://img.shields.io/badge/API-TMDB-01b4e4)

> **Sua coleção, suas regras.** Organize os jogos que você zerou, os filmes que assistiu e as séries que maratonou em um único lugar seguro e moderno.

---

## 🚀 Sobre o Projeto

O **Meus Backlog** é uma aplicação **Fullstack** robusta que simula um ambiente de produção real. A versão atual (**V6.0**) marca a maior evolução do sistema, transformando-o de um gerenciador pessoal em uma verdadeira **Rede Social de Colecionadores**, com perfis públicos, sistema de buscas e autenticação blindada com verificação de e-mail.

### ✨ Destaques da Versão 7.0 (Inteligência Artificial & Gamificação)
* 🤖 **Dual-Engine AI (Mecanismo de Failover):** Sistema de recomendação inteligente integrado nativamente via chamadas REST HTTP puras. Utiliza o **Google Gemini 2.5 Flash** como motor principal e conta com uma arquitetura de contingência (*Failover redundante*) direcionada para a infraestrutura da **Groq Cloud (Meta Llama 3.1)** caso o servidor principal sofra instabilidades (erros 503/429), garantindo disponibilidade contínua de forma 100% transparente para o usuário.
* 🏆 **Sistema de Conquistas & Gamificação:** Motor de engajamento assíncrono utilizando `@Async` do Spring para computar o progresso do usuário sem travar a thread principal de renderização. O sistema conta com um catálogo inicial de **15 Conquistas/Badges** dinâmicas (computando itens adicionados, mídias concluídas, consistência de maratonas, notas e uso de IA) e uma curva de progressão geométrica de níveis baseada em XP acumulado.
* 📜 **Parser Customizado de Markdown:** Implementação de um decodificador baseado em expressões regulares (Regex) nativo no JavaScript para ler as respostas ricas em Markdown enviadas pelas IAs e convertê-las em blocos estruturados de HTML (`<strong>`, `<ul>`), renderizados dinamicamente através de um efeito máquina de escrever.
* 💻 **Premium UI/UX Adaptativa:** Redesenho completo do fluxo de ações. No desktop, os recursos de IA ficam abrigados em um *Popover flutuante balão* isolado para não poluir o cabeçalho. No mobile, o botão é omitido de forma cirúrgica e as opções de IA são centralizadas na base da *Bottom Sheet (Gaveta)* nativa do smartphone.

### 🌟 Funcionalidades Consolidadas
* 🌐 **Rede Social (V6.0):** Perfis públicos compartilháveis (`/u/username`) no formato Read-Only e busca social com técnica de *Debounce* no JavaScript.
* 📧 **Segurança de Cadastro (V6.0):** Fluxo com geração de tokens temporários (`EmailVerificationToken`) para validação de contas via API do **Brevo** e proteção contra abusos usando `RateLimitService`.
* 📄 **Exportação de Relatórios (V5.0):** Geração em tempo real de arquivos em PDF (via OpenPDF) e planilhas Excel (via Apache POI) com processamento feito 100% em memória RAM via `ByteArrayOutputStream`.
* 📊 **Dashboard de BI (V4.0):** Gráficos analíticos interativos usando Chart.js para distribuição de acervo, status e notas.
* 🎬 **Busca Automática de Capas (V3.0):** Integração com a API do **TMDB** para carregamento automático de pôsteres e metadados.

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

## 📱 App Instalável (PWA) e Android (Capacitor)

O Meus Backlog é um **PWA** (pode ser instalado direto do navegador, com ícone, tela cheia e cache offline do app shell).

Também há uma configuração pronta para empacotar o mesmo site como **app Android nativo** via [Capacitor](https://capacitorjs.com/), permitindo publicação na Google Play. Veja o passo a passo completo em [`CAPACITOR_SETUP.md`](./CAPACITOR_SETUP.md).

---

## 🛣️ Roadmap & Futuro

- [x] V1.0: CRUD Básico.
- [x] V2.0: Login, Segurança e Perfil.
- [x] V3.0: Integração TMDB e Capas.
- [x] V3.1: Modo Social e Monitoramento (Sentry/Umami).
- [x] V4.0: Dashboard de Estatísticas (Chart.js + DTOs).
- [x] V5.0: Exportação de Relatórios (Gerar PDF/Excel da coleção).
- [x] V6.0: Rede Social (Busca dinâmica, Perfis com @ e Verificação de E-mail).
- [x] V7.0: Motores de IA com Failover Resiliente e Sistema Completo de Conquistas e Níveis.

---

## 🤝 Autor

Desenvolvido com 🤍 e ☕ por **Luiz Augusto**.
*Técnico em Informática (SENAC) - Full Stack Developer & IT Technician*
*Engenharia de Software | Graduando na Universidade Federal de Lavras (UFLA) - São Sebastião do Paraíso - MG*

---
