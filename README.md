# 🎮 Backlog API - Games & Movies

API RESTful desenvolvida para gerenciar um catálogo pessoal de jogos, filmes e séries. O objetivo é permitir o cadastro (`Create`), listagem (`Read`), atualização (`Update`) e remoção (`Delete`) de itens, incluindo sistema de notas e resenhas.

Este projeto faz parte da minha trilha de aprendizado em **Engenharia de Software**, focando em arquitetura Backend com Java e Spring Boot.

## 🚀 Tecnologias Utilizadas

* **Java 17+** (Linguagem Core)
* **Spring Boot 3** (Framework Principal)
* **Spring Data JPA** (Persistência de Dados)
* **MySQL** (Banco de Dados Relacional)
* **Lombok** (Produtividade e redução de código)
* **Maven** (Gerenciamento de Dependências)

## ⚙️ Funcionalidades (Endpoints)

A API roda localmente na porta `8080`.

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/itens` | Lista todos os itens cadastrados |
| `POST` | `/itens` | Cadastra um novo jogo ou filme |
| `PUT` | `/itens/{id}` | Atualiza os dados de um item existente |
| `DELETE` | `/itens/{id}` | Remove um item do catálogo |

### Exemplo de JSON (Payload)
```json
{
  "titulo": "The Last of Us",
  "tipo": "Jogo",
  "status": "Zerado",
  "nota": 10,
  "resenha": "Uma experiência narrativa única."
}
