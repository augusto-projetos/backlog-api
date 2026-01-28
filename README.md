# 🎮 Meu Backlog Pessoal - API & Frontend

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-blue)
![Docker](https://img.shields.io/badge/Docker-Available-blue)
![Render](https://img.shields.io/badge/Deploy-Render-black)
![License](https://img.shields.io/badge/License-MIT-yellow)

> Uma aplicação Fullstack para gerenciar listas de jogos e filmes (Backlog), com foco em organização, validação de dados e interface responsiva.

## 🔗 Demo Online
Acesse o projeto rodando em tempo real na nuvem:
👉 **[https://avaliacao-backlog.onrender.com](https://avaliacao-backlog.onrender.com)**
*(Nota: Como utilizamos o plano gratuito do Render, a primeira requisição pode levar até 3 minutos para "acordar" o servidor. As próximas são instantâneas.)*

---

## 🚀 Funcionalidades

- **CRUD Completo:** Criação, Leitura, Atualização e Exclusão de itens.
- **Validação de Dados:** Backend blindado com Bean Validation (`@NotBlank`, `@Min`, `@Max`) para impedir dados inconsistentes.
- **Interface Responsiva:** Layout otimizado para Mobile (Grid Layout) e Desktop.
- **Segurança:** Proteção contra XSS (Sanitização de URLs de imagem) e CodeQL Scans.
- **Feedback Visual:** Integração com **SweetAlert2** para notificações modernas (sucesso/erro).
- **Deploy Dockerizado:** Configuração de `Dockerfile` multi-stage para build e deploy otimizados.

---

## 🛠️ Tecnologias Utilizadas

### Backend
- **Java 17** & **Spring Boot 3**
- **Maven** (Gerenciamento de dependências)
- **Spring Data JPA** (Persistência de dados)
- **Bean Validation** (Regras de negócio)
- **H2 Database** (Testes) / **PostgreSQL** (Produção)

### Frontend
- **HTML5 & CSS3** (Grid & Flexbox)
- **JavaScript (ES6+)** (Fetch API para comunicação com Backend)
- **SweetAlert2** (Biblioteca de alertas)

### DevOps & Infraestrutura
- **Docker** (Containerização)
- **Render.com** (Hospedagem Nuvem)
- **UptimeRobot** (Monitoramento de disponibilidade)
- **GitHub Actions** (Verificação de segurança com CodeQL)

---

👨‍💻 Autor <br>
Desenvolvido por Luiz Augusto.
