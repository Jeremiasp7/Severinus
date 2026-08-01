# Severinus

API de marketplace de serviços informais ("bicos"). Trabalhadores publicam
serviços, clientes enviam propostas, o aceite gera um contrato e as partes
conversam por chat em tempo real.

> **Status:** em reconstrução. A aplicação ainda não sobe — há erros de
> mapeamento JPA em aberto, corrigidos a partir da Fase 1. Ver
> [`docs/PLANO.md`](docs/PLANO.md).

## Stack

Java 21 · Spring Boot 3.4.2 · Spring Data JPA · Spring Security · WebSocket STOMP
· PostgreSQL 16 · Flyway · springdoc-openapi · Lombok · Maven

## Requisitos

- JDK 21
- Docker e Docker Compose

## Setup

```bash
# 1. Variáveis de ambiente
cp .env.example .env
# gere um segredo e preencha JWT_SECRET no .env:
openssl rand -base64 48

# 2. Banco de dados
docker compose up -d

# 3. Build
./mvnw compile
```

## Comandos

| Comando | O que faz |
|---|---|
| `docker compose up -d` | Sobe o Postgres em `localhost:5435` |
| `docker compose down` | Para o banco, preservando os dados |
| `docker compose down -v` | Para o banco e **apaga os dados** |
| `./mvnw compile` | Compila |
| `./mvnw test` | Roda os testes |
| `./mvnw spring-boot:run` | Sobe a API em `localhost:8085` |

Use `-o` (offline) nos comandos Maven quando não houver dependência nova.

## Variáveis de ambiente

Todas têm default para desenvolvimento local, exceto `JWT_SECRET`.
Ver [`.env.example`](.env.example).

| Variável | Default | Descrição |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5435/pg_severinus` | URL do banco |

| `DB_USER` | `admin` | Usuário do banco |
| `DB_PASSWORD` | `admin` | Senha do banco |
| `JWT_SECRET` | — | Segredo do JWT. Mínimo de 32 bytes. **Obrigatório** |
| `CORS_ORIGINS` | `http://localhost:3000` | Origens permitidas, separadas por vírgula |
| `UPLOAD_DIR` | `uploads` | Diretório de armazenamento dos arquivos |

## Documentação da API

Com a aplicação no ar:

- Swagger UI: http://localhost:8085/swagger-ui.html
- OpenAPI JSON: http://localhost:8085/v3/api-docs

## Banco de dados

O schema é gerenciado **exclusivamente pelo Flyway**, em
`src/main/resources/db/migration`. `spring.jpa.hibernate.ddl-auto` está em
`validate` e não deve ser alterado.

Toda mudança de schema é uma migration nova. Nunca edite um `V*.sql` já aplicado.

## Documentação do projeto

| Documento | Conteúdo |
|---|---|
| [`AGENTS.md`](AGENTS.md) | Padrões de código e regras obrigatórias de contribuição |
| [`docs/PLANO.md`](docs/PLANO.md) | Plano de correção e evolução, em 15 fases |
