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

O **Meus Backlog** nasceu da necessidade de organizar o consumo de mídia pessoal. Na **Versão 2.0**, o projeto passou por uma reestruturação completa (Refactoring), migrando de arquivos estáticos para uma arquitetura robusta com Renderização no Servidor (SSR) e Segurança Avançada.

### ✨ O que há de novo na V2.0?

* 🔐 **Sistema de Login Completo:** Autenticação segura via E-mail e Senha.
* 🛡️ **Multi-Tenancy (Privacidade):** Cada usuário tem seu próprio universo. O que você cadastra, só você vê.
* 📱 **Design 100% Responsivo:** Interface moderna com tema Neon/Dark que se adapta perfeitamente a celulares e desktops.
* 🎨 **UI/UX Aprimorada:** Uso de Glassmorphism, feedbacks visuais com SweetAlert2 e ícones dinâmicos.
* 🔑 **Segurança de Dados:** Senhas criptografadas no banco de dados (BCrypt).

---

## 🛠️ Tecnologias Utilizadas

* **Back-end:** Java 17, Spring Boot 3.
* **Segurança:** Spring Security 6 (Configuração de Rotas, BCrypt, UserDetailsService).
* **Front-end:** Thymeleaf (Engine de Templates), HTML5, CSS3 (Flexbox/Grid), JavaScript (Fetch API).
* **Banco de Dados:** MySQL (Produção) / H2 (Desenvolvimento).
* **Bibliotecas Extras:** Lombok, SweetAlert2 (Alertas bonitos).

---

## ⚙️ Como Rodar Localmente

### Pré-requisitos
* Java JDK 17 ou superior.
* Maven instalado.
* MySQL instalado (ou usar o H2 em memória).

### Passo a Passo

1.  **Clone o repositório:**
    ```bash
    git clone [https://github.com/seu-usuario/meus-backlog.git](https://github.com/seu-usuario/meus-backlog.git)
    ```
2.  **Configure o Banco de Dados:**
    No arquivo `src/main/resources/application.properties`, ajuste as credenciais:
    ```properties
    spring.datasource.url=jdbc:mysql://localhost:3306/backlog_db
    spring.datasource.username=seu_usuario
    spring.datasource.password=sua_senha
    
    # Dica: Na primeira execução, use 'update' ou 'create-drop' se precisar limpar
    spring.jpa.hibernate.ddl-auto=update
    ```
3.  **Execute o Projeto:**
    ```bash
    mvn spring-boot:run
    ```
4.  **Acesse:**
    Abra o navegador em `http://localhost:8080`.

---

## 📂 Estrutura do Projeto

O código segue o padrão **MVC (Model-View-Controller)**:

* `controller`: Gerencia as requisições (Web e API).
* `service`: Regras de negócio (ex: Autenticação).
* `repository`: Comunicação direta com o banco de dados.
* `entity`: Modelos das tabelas (User, Item).
* `dto`: Objetos de transferência de dados (Login, Registro).
* `security`: Configurações de proteção e filtros.

---

## 🤝 Autor

Desenvolvido por **Luiz Augusto**. <br>
*Técnico em Informática*

---
