# 🎮 Meu Backlog Pessoal - API & Frontend

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3-green)
![Spring Security](https://img.shields.io/badge/Spring_Security-6-6db33f)
![Chart.js](https://img.shields.io/badge/Frontend-Chart.js-FF6384)
![Render](https://img.shields.io/badge/Deploy-Render-black)
![License](https://img.shields.io/badge/License-MIT-yellow)
![TMDB](https://img.shields.io/badge/API-TMDB-01b4e4)

> **Sua coleção, suas regras.** Organize os jogos que você zerou, os filmes que assistiu e as séries que maratonou em um único lugar seguro e moderno.

---

## 🚀 Sobre o Projeto

O **Meus Backlog** é uma aplicação **Fullstack** robusta que simula um ambiente de produção real. A versão atual (**V5.0**) eleva o nível do projeto adicionando recursos de **Relatórios Gerenciais**, permitindo que o usuário extraia seus dados para uso externo, além de manter a camada de **Business Intelligence (BI)** para visualização em tempo real.

### ✨ Destaques da Versão 5.0 (Atual)
* 📄 **Exportação de Dados (Relatórios):** Funcionalidade corporativa essencial. Agora é possível baixar todo o acervo em **PDF** (com layout formatado para impressão) ou **Excel** (planilha estruturada para análise de dados externa).
* ⚡ **Processamento em Memória:** A geração dos arquivos utiliza `ByteArrayOutputStream` e bibliotecas otimizadas (**Apache POI** e **OpenPDF**), processando tudo na memória RAM para entregar o download instantaneamente, sem onerar o disco do servidor.
* 🛡️ **Blindagem contra Falhas:** Tratamento robusto de dados nulos e formatação condicional na geração dos documentos, garantindo que o relatório nunca quebre, mesmo com informações incompletas.

### 🌟 Funcionalidades Consolidadas
* 📊 **Dashboard de BI:** Gráficos interativos (Chart.js) com distribuição de acervo, status de progresso e histograma de notas.
* 🌐 **Modo Social:** Links públicos temporários para compartilhar sua coleção com amigos.
* 🎬 **API da TMDB:** Busca automática de capas, sinopses e metadados de filmes/séries.
* 🔍 **Filtros Avançados:** "Gaveta" de filtros com ordenação por nota, tipo e status.
* 🔐 **Segurança:** Spring Security 6, BCrypt, Proteção CSRF e Monitoramento via Sentry.

---

## 🛠️ Arquitetura & Tecnologias

### Backend (Java Ecosystem)
* **Java 17 & Spring Boot 3:** API REST e MVC.
* **Apache POI & OpenPDF:** Manipulação avançada de arquivos Office e PDFs.
* **JPA/Hibernate:** Consultas otimizadas com Projections e DTOs.
* **Spring Security:** Controle de sessão e autenticação.
* **Sentry SDK:** Monitoramento de erros em produção.

### Frontend
* **Thymeleaf:** Renderização Server-Side (SSR).
* **Chart.js:** Biblioteca de visualização de dados (Canvas).
* **JavaScript (Vanilla):** Lógica assíncrona (`async/await`) e manipulação de DOM.
* **CSS3 (Neon Theme):** Variáveis CSS para temas dinâmicos (Dark/Light Mode).

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
- [x] V3.1: Modo Social e Monitoramento (Sentry/Umami).
- [x] V4.0: Dashboard de Estatísticas (Chart.js + DTOs).
- [x] V5.0: Exportação de Relatórios (Gerar PDF/Excel da coleção).
- [ ] **V6.0:** ???

---

## 🤝 Autor

Desenvolvido com 🤍 e ☕ por **Luiz Augusto**.  
*Estudante de Engenharia de Software & Técnico em Informática*

---
