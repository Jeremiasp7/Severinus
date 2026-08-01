# Plano de Correção e Evolução — Severinus

Documento de trabalho. Estado inicial: **a aplicação não sobe**.

```
Failed to initialize JPA EntityManagerFactory: Association
'com.severinus.modules.chat.entities.ChatEntity.utilizador1' targets the type
'java.util.UUID' which is not an '@Entity' type
```

Objetivo: sair de um esqueleto que não inicializa para uma API de marketplace de
serviços informais ("bicos") funcional, com autenticação, chat e o fluxo de
contratação.

---

## Decisões tomadas

| Tema | Decisão |
|---|---|
| Modelo de usuário | Unificar em `UserEntity` + `WorkerProfile` 1-1 opcional |
| Autenticação | JWT stateless + BCrypt, com refresh token revogável e logout real |
| Domínio bico | `Servico` → `Proposta` → `Contrato` → `Avaliacao` |
| Schema | Flyway, `ddl-auto=validate`, **banco criado do zero, sem baseline** |
| Connection | Mantido. Chat abre por conexão aceita **OU** contrato ativo |
| Storage | Interface `StorageService`, impl local agora, S3 depois |
| Padrões de código | `AGENTS.md` aplicado **retroativamente durante as Fases 1–7** |
| Notificações | In-app (WebSocket) + e-mail, com preferência por evento e canal |
| Dados pessoais | LGPD essencial + PII cifrada/mascarada + verificação de identidade |
| Anti-abuso | Rate limiting, moderação, sanitização, upload blindado |
| Infra | CORS e headers de segurança apenas. CI/SAST/backup ficam no backlog |

> **Banco do zero:** não haverá `baseline-on-migrate`. O volume do Postgres é
> descartado e `V1__init.sql` cria o schema inteiro. Dados existentes em dev se
> perdem — isso é intencional e aceito.

> **Conformidade retroativa com `AGENTS.md`:** não haverá uma fase separada de
> retrofit. Todo arquivo tocado nas Fases 1–7 sai **em conformidade total** antes
> de a fase ser dada como concluída:
>
> - service criado ou alterado → teste unitário (JUnit 5 + Mockito, caminho feliz
>   e casos de erro) na mesma fase;
> - controller criado ou alterado → `@Tag`, `@Operation` e `@ApiResponse` de todos
>   os códigos reais;
> - classe ou método público tocado → Javadoc em português.
>
> Como as Fases 1–7 reescrevem praticamente todo o código atual, ao fim da Fase 7
> o projeto inteiro está em conformidade. A varredura de confirmação fica na
> Fase 8.

---

## Inventário de problemas

### Bloqueadores de boot

| # | Local | Problema |
|---|---|---|
| B1 | `ChatEntity.java:30-36`, `PostsEntity.java:42-44`, `ConnectionEntity.java:29-35`, `MessageEntity.java:35-41` | `@ManyToOne` aplicado a campo `UUID`. Hibernate aborta o bootstrap. |
| B2 | `ChatEntity.java:31,35` | `utilizador1` e `utilizador2` declaram o mesmo `@JoinColumn(name="utilizador_id")`. Coluna duplicada. |
| B3 | `PostsRepository.java:14` | `findByUsuarioId` — não existe campo `usuario`; o campo é `utilizador`. |
| B4 | `ChatRepository.java:11` | `findByConsumer1AndConsumer2` — campos são `utilizador1`/`utilizador2`. |
| B5 | `ConnectionRepository.java:15` | `findBySolicitanteAndRecebedorAndStatus` — campos são `solicitanteId`/`recebedorId`. |
| B6 | `WorkerEntity.java:44-45`, `PostsEntity.java:36-37` | `List<String>` sem `@ElementCollection`. Não persiste. |

> B3–B5 só se manifestam depois que B1 for corrigido: o bootstrap morre antes de
> chegar na validação das derived queries.

### Bugs de lógica

| # | Local | Problema |
|---|---|---|
| L1 | `ConnectionService.java:31` | `findByIdAndStatus(id, PENDENTE)` filtra pelo **PK da conexão**, não pelo destinatário. Deveria ser `findByRecebedorIdAndStatus`. |
| L2 | `ChatService.java:43-47` | Busca lista `PENDENTE` e depois testa `status == ACEITA`. Condição nunca satisfeita → `criarChat` sempre retorna `null`. |
| L3 | `WorkerService.java:19-25` | Nunca popula `certificados` → `getCertificados()` retorna `null` → NPE em `FileStorageController.java:62`. |
| L4 | `WebSocketConfig.java:20` | Broker registra só `/topic`, mas `ChatService.java:80` envia para `/queue/messages`. Mensagem nunca é entregue. |
| L5 | `ChatService.java:80` | `convertAndSendToUser` exige `Principal` autenticado. Não há autenticação no WebSocket. |
| L6 | `ChatController.java:35` | `message.getChat().getId()` — o payload de entrada não traz `chat` → NPE. |
| L7 | `ChatController.java:34-35` | Envio duplicado: o service já publicou a mensagem, o controller publica de novo. |
| L8 | `PostsController.java:54` | `@RequestBody` + `@RequestParam MultipartFile[]` na mesma requisição multipart. Não faz bind. |
| L9 | `PostsController.java:83-84` | Rota `/get/{userId}` mas parâmetro anotado `@RequestParam`. Deveria ser `@PathVariable`. |
| L10 | `ChatService.java:62-66` | `findByConsumer1AndConsumer2` pode retornar `null` nas duas tentativas; `chat` null é gravado sem checagem, violando `nullable=false`. |

### Segurança

| # | Local | Problema |
|---|---|---|
| S1 | `UserService.java:24`, `WorkerService.java:24` | Senha gravada em texto puro. |
| S2 | `SecurityConfig.java:19` | `anyRequest().permitAll()` — nenhum endpoint protegido. |
| S3 | `UserController.java:31`, `WorkerController.java:32` | Retornam a entidade crua → `password` e `cpf` no JSON de resposta. |
| S4 | `FileStorageController.java:70`, `PostsController.java:63` | `resolve(fileName)` com nome vindo do cliente. `cleanPath` normaliza mas não impede escapar de `uploads/`. |
| S5 | `FileStorageController.java:98` | Mesmo problema no download. |
| S6 | `application.properties:3-4` | Credenciais do banco hard-coded e versionadas. |
| S7 | — | Sem rate limiting no login/registro (relevante a partir da Fase 3). |

---

## Fase 0 — Preparação ✅

**Meta:** dependências e configuração prontas; banco recriado vazio.

- [x] Adicionar ao `pom.xml`:
  - `spring-boot-starter-validation`
  - `flyway-core` + `flyway-database-postgresql`
  - `spring-boot-starter-oauth2-resource-server` (JWT via Nimbus, sem lib externa)
  - `springdoc-openapi-starter-webmvc-ui` 2.8.3 (Swagger — obrigatório por `AGENTS.md`)
  - `mockito-junit-jupiter` — confirmado: já vem via `spring-boot-starter-test`
- [x] Corrigir a indentação e remover o `<version>3.4.4</version>` explícito do
      `spring-boot-starter-websocket` — o parent BOM já fixa 3.4.2.
- [x] `application.properties` com `ddl-auto=validate`, Flyway, credenciais por
      variável de ambiente, JWT e springdoc.
- [x] `OpenApiConfig` (`com.severinus.config`) com metadados e o `securityScheme`
      `bearerAuth` do tipo HTTP/JWT.
- [x] Criar `.env.example` documentando as variáveis. Não versionar `.env`.
- [x] `.gitignore`: adicionar `uploads/`, `.env` e `.env.*` (com exceção para
      `.env.example`).
- [x] Recriar o banco vazio.
- [x] `README.md` com setup, comandos e tabela de variáveis de ambiente.

**Aceite:** ✅ `./mvnw -o compile` passa. Container do Postgres no ar e saudável,
banco vazio, Flyway conectando (`Successfully validated 0 migrations`). O único
erro restante no boot é o de mapeamento JPA, alvo da Fase 1.

### Ajustes feitos durante a execução

**Porta do banco: 5434 → 5435.** A máquina de desenvolvimento tem três instâncias
**nativas** do PostgreSQL rodando como serviço do Windows
(`postgresql-x64-16`, `-17`, `-18`), ocupando as portas 5432, 5433 e 5434. O
container publicava em 5434 e era silenciosamente sombreado pela instalação
nativa — daí o `FATAL: autenticação do tipo senha falhou para o usuário "admin"`
que aparecia mesmo com o container saudável e com `docker exec psql` funcionando.

Esse era o erro observado já na primeira execução dos testes, antes de qualquer
alteração. Decidido mover o Severinus para a 5435 (livre) em vez de mexer nos
serviços nativos, que podem atender outros projetos.

Refletido em: `docker-compose.yml`, `application.properties`, `.env.example`,
`README.md`.

**Melhorias no `docker-compose.yml`:**

- removido o atributo `version`, obsoleto no Compose v2 (emitia warning a cada comando);
- imagem fixada em `postgres:16` — `postgres` sem tag pega `latest` e quebra
  reprodutibilidade;
- **volume nomeado** `severinus_pgdata` no lugar de volume anônimo, para os dados
  sobreviverem a `docker compose down`;
- `healthcheck` com `pg_isready`, permitindo aguardar o banco ficar pronto;
- credenciais por variável de ambiente, com default de desenvolvimento.

---

## Fase 1 — Modelo de domínio unificado

**Meta:** resolver B1, B2, B6 e a duplicação User/Worker.

### 1.1 `UserEntity` unificado

```
users
 ├ id            UUID PK
 ├ cpf           unique, not null
 ├ nome_completo
 ├ email         unique, not null
 ├ nome_usuario  unique, not null
 ├ senha_hash    not null
 ├ criado_em
 └ workerProfile 1-1 opcional (mappedBy)
```

- [ ] Renomear o campo `password` → `senhaHash` para deixar explícito que não é
      texto puro.
- [ ] Remover `@Data` das entidades JPA. `@Data` gera `equals`/`hashCode` sobre
      todos os campos, incluindo coleções `LAZY` — causa `LazyInitializationException`
      e recursão infinita em relações bidirecionais. Trocar por `@Getter`/`@Setter`
      + `equals`/`hashCode` sobre o `id`.

### 1.2 `WorkerProfileEntity` (1-1 opcional)

```
worker_profiles
 ├ id          UUID PK
 ├ user_id     UUID FK unique → users.id
 ├ cpf_cnpj    unique
 ├ bio
 ├ profissoes  → worker_profile_profissoes (@ElementCollection)
 └ certificados → worker_profile_certificados (@ElementCollection)
```

- [ ] Renomear `profissões` → `profissoes` (campo com acento em identificador Java).
- [ ] `@ElementCollection` nas duas listas (corrige B6).
- [ ] Inicializar as listas com `new ArrayList<>()` no builder (`@Builder.Default`)
      para nunca serem `null` (corrige L3).

### 1.3 Corrigir associações

- [ ] `PostsEntity.utilizador` → `@ManyToOne UserEntity autor` + `@JoinColumn(name="autor_id")`
- [ ] `ConnectionEntity.solicitanteId/recebedorId` → `@ManyToOne UserEntity solicitante/recebedor`
- [ ] `ChatEntity.utilizador1/utilizador2` → `@ManyToOne UserEntity`, com
      `@JoinColumn` **distintos**: `usuario1_id` e `usuario2_id` (corrige B2)
- [ ] `MessageEntity.remetente/destinatario` → `@ManyToOne UserEntity`
- [ ] `PostsEntity.tags` → `@ElementCollection`
- [ ] Todas as `@ManyToOne` com `fetch = FetchType.LAZY` (o padrão é EAGER e gera
      N+1 no feed).

### 1.4 Remover a duplicação

- [ ] Excluir `WorkerEntity`, `WorkerRepository`, `WorkerService`, `WorkerController`.
- [ ] `POST /worker` vira `POST /users/me/worker-profile` (usuário autenticado
      ativa o perfil de trabalhador).
- [ ] `GET /worker/workers` vira `GET /workers` com filtro por profissão
      (implementado na Fase 6).

**Aceite:** `./mvnw -o compile` passa; nenhuma referência a `WorkerEntity` sobra.

---

## Fase 2 — Flyway `V1__init.sql`

**Meta:** schema versionado, `ddl-auto=validate` verde.

- [ ] Criar `src/main/resources/db/migration/V1__init.sql` com o schema completo
      da Fase 1: `users`, `worker_profiles`, `worker_profile_profissoes`,
      `worker_profile_certificados`, `posts`, `post_tags`, `post_image_paths`,
      `connection`, `chats`, `messages`.
- [ ] Índices: `users(email)`, `users(nome_usuario)`, `users(cpf)`,
      `connection(recebedor_id, status)`, `messages(chat_id, data_envio)`,
      `posts(autor_id, data_post DESC)`.
- [ ] Constraints: `chats` com `UNIQUE(usuario1_id, usuario2_id)` e
      `CHECK (usuario1_id <> usuario2_id)`.

> Para gerar o DDL de referência e comparar com o SQL escrito à mão:
> `spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create`
> em um perfil descartável. Não deixar isso ligado.

**Aceite:** app sobe. `ddl-auto=validate` não reclama. Este é o marco em que o
erro do topo deste documento desaparece.

---

## Fase 3 — Autenticação (JWT + BCrypt)

**Meta:** resolver S1, S2, S3.

- [ ] `PasswordEncoder` como `@Bean` (`BCryptPasswordEncoder`).
- [ ] `AuthController`:
  - `POST /auth/register` — cria usuário com senha hasheada
  - `POST /auth/login` — valida credenciais, devolve access + refresh token
  - `POST /auth/refresh` — troca refresh válido por novo par de tokens
  - `POST /auth/logout` — revoga o refresh token
- [ ] `JwtService`: emissão e validação. Claims: `sub` = user id, `username`, `exp`.
- [ ] **Refresh token revogável.** JWT puro não tem logout — um token vazado vale
      até expirar. Mitigação: access token de vida curta (15 min) + refresh
      persistido e revogável (30 dias).
  ```
  refresh_tokens
   ├ id, user_id FK
   ├ token_hash   (hash, nunca o token em claro)
   ├ expira_em, revogado_em
   └ user_agent, ip
  ```
  - Rotação: cada `/auth/refresh` revoga o token usado e emite um novo.
  - Reúso de token já revogado → revogar toda a cadeia daquele usuário (sinal de
    roubo de token).
- [ ] `SecurityConfig` — substituir `anyRequest().permitAll()`:
  ```
  permitAll:     POST /auth/**, GET /actuator/health
  authenticated: todo o resto
  ```
- [ ] `UserDetailsService` carregando por `nomeDeUsuario`.
- [ ] **DTOs de saída.** Criar `UserResponseDto` sem `senhaHash` e sem `cpf`.
      Nenhum controller devolve entidade crua (corrige S3).
- [ ] Validação: `@Valid` + `@NotBlank`/`@Email`/`@Size` nos DTOs de entrada.
- [ ] `@ControllerAdvice` global: `MethodArgumentNotValidException` → 400,
      `EntityNotFoundException` → 404, `AccessDeniedException` → 403. Substituir
      os `throw new RuntimeException()` sem mensagem
      (`FileStorageController.java:52,60`, `PostsController.java:48`).

### CORS e headers de segurança

Pré-requisito para qualquer frontend consumir a API.

- [ ] `CorsConfigurationSource` com origens vindas de
      `app.cors.allowed-origins=${CORS_ORIGINS:http://localhost:3000}`.
      Nunca `*` junto com credenciais.
- [ ] Headers: HSTS, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`,
      `Referrer-Policy: no-referrer`.
- [ ] `V2__refresh_tokens.sql`.

**Aceite:** requisição sem token em endpoint protegido → 401. Senha nunca aparece
em resposta nem em texto puro no banco. Logout invalida o refresh token de fato.
Frontend em `localhost:3000` consegue chamar a API.

> **Numeração das migrations.** A partir daqui cada fase que mexe em schema
> consome o próximo número. A ordem final fica registrada na tabela ao fim deste
> documento — confira antes de criar um `V*.sql` novo.

---

## Fase 4 — Storage seguro

**Meta:** resolver S4, S5.

- [ ] Interface `StorageService`:
  ```java
  String store(MultipartFile file, String subPasta);
  Resource load(String chave);
  void delete(String chave);
  ```
- [ ] `LocalStorageService` implementando, com:
  - **nome gerado no servidor**: `UUID.randomUUID() + extensão validada`
    (nunca `getOriginalFilename()`)
  - allowlist de extensão/MIME: imagens em posts, `pdf`/imagem em certificados
  - verificação explícita de que o path resolvido está contido no diretório base:
    ```java
    Path alvo = base.resolve(nome).normalize();
    if (!alvo.startsWith(base)) throw new StorageException("path inválido");
    ```
  - limite de tamanho por arquivo e de quantidade por requisição
- [ ] Reescrever `FileStorageController` e a parte de upload de `PostsController`
      para usarem o service. Nenhum acesso direto a `Paths`/`Files` em controller.
- [ ] Autorização: só o dono pode subir certificado no próprio perfil.
      Hoje `POST /api/files/upload/certificates/{userId}` aceita qualquer `userId`.
- [ ] Corrigir `FileStorageController.java:72-78`: o método adiciona **duas**
      entradas por arquivo (o path absoluto do disco *e* a URL de download) na
      mesma lista `certificados`. Guardar só a chave; montar a URL na resposta.

**Aceite:** upload com nome `../../evil.txt` é rejeitado ou gravado dentro de
`uploads/` com nome novo. Usuário A não consegue subir arquivo no perfil de B.

---

## Fase 5 — Correção de Posts e Connection

**Meta:** resolver L1, L8, L9, B3, B5.

- [ ] `PostsController.criarPost`: trocar `@RequestBody`+`@RequestParam` por
      `@RequestPart("dados") CreatePostDto` + `@RequestPart("imagens") MultipartFile[]`,
      com `consumes = MULTIPART_FORM_DATA_VALUE` (corrige L8).
- [ ] Remover `userId` da rota — o autor vem do token, não do path. Hoje qualquer
      um posta em nome de qualquer um.
- [ ] `PostsController.buscarPosts`: `@PathVariable` (corrige L9).
- [ ] `PostsRepository.findByUsuarioId` → `findByAutorId` (corrige B3).
- [ ] `ConnectionRepository`: `findByRecebedorIdAndStatus` e
      `findBySolicitanteAndRecebedorAndStatus` com os nomes de campo reais (B5).
- [ ] `ConnectionService.buscarSolicitacoes` passa a usar
      `findByRecebedorIdAndStatus(userId, PENDENTE)` (corrige L1).
- [ ] Adicionar o que falta em Connection: `PATCH /connection/{id}` para
      aceitar/recusar. Hoje só existe criar e listar — não há como uma conexão
      chegar a `ACEITA`, o que trava o chat.
- [ ] Impedir conexão duplicada e auto-conexão.
- [ ] Paginação (`Pageable`) em listagens de posts e conexões.

**Aceite:** ciclo completo solicitar → aceitar → listar conexões funciona.

---

## Fase 6 — Domínio "bico"

**Meta:** o núcleo do produto.

```
servicos
 ├ id, worker_profile_id FK
 ├ titulo, descricao
 ├ categoria         (enum)
 ├ preco_base, unidade_preco  (HORA | DIARIA | EMPREITADA)
 ├ cidade, uf
 └ ativo, criado_em

propostas
 ├ id, servico_id FK, cliente_id FK → users
 ├ mensagem, valor_proposto
 ├ status  (PENDENTE | ACEITA | RECUSADA | CANCELADA)
 └ criado_em

contratos
 ├ id, proposta_id FK unique
 ├ status  (ACEITO | EM_ANDAMENTO | CONCLUIDO | CANCELADO)
 ├ valor_final
 └ iniciado_em, concluido_em

avaliacoes
 ├ id, contrato_id FK
 ├ autor_id FK, avaliado_id FK → users
 ├ nota (1..5), comentario
 └ criado_em
```

- [ ] `V3__dominio_servicos.sql` com as quatro tabelas.
- [ ] Entidades, repositories, services, controllers.
- [ ] Máquina de estados do contrato — transições válidas explícitas, rejeitar o
      resto. `ACEITO → EM_ANDAMENTO → CONCLUIDO`; `CANCELADO` a partir dos dois
      primeiros.
- [ ] Regras de autorização: só o cliente aceita/cancela proposta que ele criou;
      só o trabalhador dono do serviço responde propostas daquele serviço.
- [ ] Avaliação só após `CONCLUIDO`, uma por parte por contrato
      (`UNIQUE(contrato_id, autor_id)`).
- [ ] Nota média denormalizada em `worker_profiles.nota_media` +
      `total_avaliacoes`, atualizada na conclusão. Evita `AVG` a cada listagem.
- [ ] Busca: `GET /servicos?categoria=&cidade=&uf=&precoMax=&q=` com `Pageable`.
      Usar `Specification` para os filtros opcionais.
- [ ] Ativar `PostsRepository.findByTags`, hoje declarado e nunca usado.

**Aceite:** fluxo completo publicar serviço → propor → aceitar → contrato →
concluir → avaliar, com as regras de autorização respeitadas.

---

## Fase 7 — Chat funcional

**Meta:** resolver L2, L4, L5, L6, L7, L10, B4.

- [ ] `WebSocketConfig`: `registry.enableSimpleBroker("/topic", "/queue")` (L4).
- [ ] Autenticar o WebSocket: `ChannelInterceptor` no `CONNECT` lendo o JWT do
      header STOMP e populando o `Principal` (L5). Sem isso
      `convertAndSendToUser` não resolve destinatário.
- [ ] `ChatEntity` ganha `contrato_id` nullable (`V4__chat_contrato.sql`).
- [ ] Regra de abertura de chat (substitui L2):
  ```
  pode conversar se:
      connection(a,b).status == ACEITA
      OU existe contrato ativo entre a e b
  ```
- [ ] `ChatRepository.findByConsumer1AndConsumer2` → `findByUsuario1AndUsuario2` (B4).
  Melhor: uma única query que cobre as duas ordens —
  ```java
  @Query("""
      select c from chats c
      where (c.usuario1.id = :a and c.usuario2.id = :b)
         or (c.usuario1.id = :b and c.usuario2.id = :a)
      """)
  ```
  Elimina a dupla chamada de `ChatService.java:62-66`.
- [ ] `mandarMensagem`: se o chat não existe, criar (respeitando a regra acima)
      em vez de gravar `chat = null` (L10).
- [ ] Remetente vem do `Principal`, não do payload. Hoje dá para forjar
      `remetente` no corpo da mensagem e enviar como outra pessoa.
- [ ] `ChatController.processMessage`: remover o segundo envio (L7) e a
      dereferência de `message.getChat()` (L6). Usar um `ChatMessageDto` de
      entrada com `destinatarioId` + `conteudo`, nada mais.
- [ ] `mensagemLida`: endpoint para marcar como lida e contador de não lidas.
- [ ] `GET /api/chat/history/{chatId}` — validar que o usuário autenticado
      participa do chat, e paginar.

**Aceite:** dois usuários conectados trocam mensagens em tempo real; histórico
persiste; terceiro não consegue ler o chat alheio.

---

## Fase 8 — Qualidade

> **Teste unitário de service não é Fase 8.** Por `AGENTS.md`, todo service
> criado ou alterado sai com teste unitário (JUnit 5 + Mockito, caminho feliz e
> casos de erro) **na mesma fase em que é escrito**. O mesmo vale para Javadoc em
> português e anotações Swagger nos controllers. Esta fase cobre o que sobra.

- [ ] Testes de integração com Testcontainers (Postgres real, não H2 — o schema
      é validado contra Flyway).
- [ ] Cobrir: registro/login, autorização por dono, máquina de estados do
      contrato, path traversal no upload, regra de abertura de chat.
- [ ] Varredura final: nenhum service sem teste, nenhum endpoint sem `@Operation`,
      nenhuma classe pública sem Javadoc.
- [ ] `SeverinusApplicationTests.contextLoads` hoje está vazio e o contexto nem
      sobe. Passa a ser um teste real.
- [ ] `spring-boot-starter-actuator` com `/health` exposto.
- [ ] Logging estruturado; nunca logar token ou senha.
- [ ] README com instruções de setup.

---

# Parte II — Produto e proteção de dados

As Fases 0–8 entregam uma API que sobe, autentica e executa o fluxo de
contratação. As fases a seguir são o que transforma isso em um produto que as
pessoas querem usar e que trata dado pessoal de forma responsável.

Cada fase declara de qual anterior depende. Fases sem dependência entre si podem
ser feitas em paralelo ou reordenadas conforme prioridade.

---

## Fase 9 — LGPD e proteção de PII

**Depende de:** Fase 3 (auth)
**Por quê:** o sistema trata CPF, e-mail, telefone e localização. Isso é obrigação
legal, não funcionalidade. Quanto mais tarde, mais dado exposto para retrofit.

### 9.1 Consentimento

- [ ] `V5__lgpd.sql` com:
  ```
  consentimentos
   ├ id, user_id FK
   ├ versao_termos
   ├ aceito_em, ip, user_agent
   └ finalidade  (USO_PLATAFORMA | MARKETING | ...)
  ```
- [ ] Registro no `POST /auth/register` com a versão vigente dos termos.
- [ ] Revogação de consentimentos não essenciais.
- [ ] Nova versão dos termos exige novo aceite.

### 9.2 Direitos do titular (art. 18)

- [ ] `GET /me/dados` — exportação completa em JSON: perfil, posts, conexões,
      serviços, propostas, contratos, avaliações, mensagens.
- [ ] `DELETE /me` — **anonimização, não `DELETE`**. Contratos e avaliações
      precisam sobreviver por integridade histórica e contábil:
  ```
  users.cpf          → NULL
  users.email        → 'removido-<uuid>@severinus.invalid'
  users.nome_completo→ 'Usuário removido'
  users.nome_usuario → 'usuario_<uuid curto>'
  users.senha_hash   → NULL, conta desativada
  users.anonimizado_em → now()

  posts, mensagens   → conteúdo apagado, registro mantido
  contratos          → preservados integralmente
  avaliacoes         → texto mantido, autor anonimizado
  ```
- [ ] Coluna `anonimizado_em` bloqueia login e some das listagens.

### 9.3 Retenção

- [ ] Política documentada por tipo de dado (ex.: log de auditoria 5 anos,
      mensagem de chat 2 anos após conclusão do contrato, token revogado 90 dias).
- [ ] Job agendado (`@Scheduled`) aplicando a política.

### 9.4 Proteção de PII

- [ ] **CPF/CNPJ cifrado em repouso** via `AttributeConverter` com AES-GCM.
      Chave por variável de ambiente, nunca no repositório.
  ```java
  @Convert(converter = CriptografiaConverter.class)
  private String cpf;
  ```
  > Campo cifrado não é pesquisável por `LIKE`. Se precisar buscar por CPF,
  > guarde também um hash determinístico (HMAC) em coluna indexada.
- [ ] **Mascaramento na resposta:** `***.456.789-**`. Valor completo só para o
      próprio titular. Implementar em serializer, não caso a caso.
- [ ] **PII fora dos logs:** conversor de log mascarando CPF, e-mail, telefone e
      token. Nunca logar `UserEntity` inteira.

### 9.5 Auditoria

- [ ] `audit_log` append-only (sem `UPDATE`, sem `DELETE`):
  ```
   ├ id, ocorrido_em
   ├ ator_id, ip, user_agent
   ├ acao      (LEITURA | CRIACAO | ALTERACAO | EXCLUSAO | EXPORTACAO)
   ├ entidade, entidade_id
   └ detalhes  jsonb
  ```
- [ ] Registrar: login (sucesso e falha), acesso a dado pessoal de terceiro,
      alteração de perfil, exportação, anonimização, aprovação de documento.
- [ ] Anotação `@Auditavel` + aspecto, para não poluir os services.

**Aceite:** titular exporta os próprios dados; anonimização não quebra nenhum
contrato existente; CPF não aparece em claro no banco nem em log.

---

## Fase 10 — Verificação de identidade e reputação

**Depende de:** Fases 6 (contratos) e 9 (PII — o documento enviado é dado sensível)
**Por quê:** é o que faz alguém contratar um desconhecido. Sem isso, o marketplace
não sai do papel por mais bem construído que esteja.

### 10.1 Verificação em níveis

- [ ] `V6__verificacao_reputacao.sql`.
- [ ] Níveis progressivos:
  ```
  NENHUM → EMAIL → TELEFONE → DOCUMENTO
  ```
- [ ] E-mail: token de uso único com expiração, enviado no registro.
- [ ] Telefone: código OTP de 6 dígitos, com limite de tentativas e reenvio.
- [ ] Documento: upload + fila de aprovação manual (`PENDENTE`/`APROVADO`/`REJEITADO`
      com motivo). Arquivo de documento **nunca** fica publicamente acessível.
- [ ] Regra: **e-mail e telefone verificados são obrigatórios para enviar proposta
      ou publicar serviço.** Navegar e conversar não exige.
- [ ] Selo exibido no perfil conforme o nível.

### 10.2 Métricas de reputação

- [ ] Denormalizadas em `worker_profiles`, atualizadas por evento:
  ```
  nota_media, total_avaliacoes
  tempo_medio_resposta_min      (proposta → primeira resposta)
  taxa_conclusao                (concluídos / aceitos)
  total_contratos_concluidos
  membro_desde
  ```
  Denormalizar evita `AVG`/`COUNT` a cada listagem — o cálculo ao vivo mata a
  performance da busca.

### 10.3 Avaliação bidirecional

- [ ] Cliente avalia trabalhador **e** trabalhador avalia cliente.
- [ ] Critérios separados: `pontualidade`, `qualidade`, `comunicacao`, além da
      nota geral. Trabalhador avaliando cliente usa `clareza_demanda` e
      `pontualidade_pagamento`.
- [ ] **Publicação cega:** a avaliação só fica visível quando ambos avaliarem ou
      quando o prazo (14 dias) expirar. Evita retaliação — quem avalia primeiro
      não é punido.
  ```
  avaliacoes
   ├ contrato_id, autor_id, avaliado_id
   ├ nota_geral + critérios
   ├ comentario
   ├ criado_em
   └ publicado_em  (NULL enquanto cega)
  ```
- [ ] `UNIQUE(contrato_id, autor_id)` — uma avaliação por parte por contrato.
- [ ] Resposta pública do avaliado ao comentário (direito de réplica).

### 10.4 Portfólio

- [ ] `portfolio_itens`: título, descrição, imagens, categoria, data.
      Separado do feed de posts — portfólio é vitrine curada, post é timeline.

**Aceite:** usuário sem telefone verificado não envia proposta; avaliação não
aparece antes de ambos avaliarem; nota média bate com a soma das avaliações.

---

## Fase 11 — Descoberta e busca

**Depende de:** Fase 6 (serviços existem)

- [ ] **Taxonomia controlada de profissões e categorias.** Tabela `categorias`
      hierárquica, em vez de texto livre. Hoje `profissoes` é `List<String>` —
      "pedreiro", "Pedreiro" e "pedrero" fragmentam a busca e inviabilizam filtro.
- [ ] **Busca geográfica por raio.** Duas opções:
  - PostGIS com `geography(Point)` e `ST_DWithin` — preciso, indexável com GIST,
    exige extensão no Postgres;
  - haversine em SQL puro com pré-filtro por bounding box — sem dependência,
    suficiente para raios pequenos.

  Recomendo PostGIS: o container Postgres pode virar `postgis/postgis` sem
  impacto no resto.
- [ ] `GET /servicos` com filtros combináveis: `q`, `categoria`, `lat`+`lng`+`raioKm`,
      `precoMin`/`precoMax`, `notaMinima`, `apenasVerificados`, ordenação
      (relevância, distância, preço, nota). Usar `Specification`.
- [ ] Busca textual: `tsvector` em português com índice GIN, com `unaccent`.
      Busca por "eletricista" precisa achar "Eletricista" e "elétrica".
- [ ] Favoritos: serviço e trabalhador.
- [ ] Filtros salvos + alerta (novo serviço compatível gera notificação — integra
      com a Fase 13).
- [ ] Feed recomendado: categoria de interesse + proximidade + reputação.
      Começar por ordenação ponderada simples; nada de ML.
- [ ] Paginação por cursor nas listagens grandes (`keyset`), não `OFFSET` —
      `OFFSET` degrada em páginas profundas.

**Aceite:** buscar "eletricista" num raio de 10 km retorna resultados ordenados
por relevância em tempo aceitável, com filtro por nota e verificação.

---

## Fase 12 — Negociação e agenda

**Depende de:** Fase 6 (propostas e contratos)

- [ ] **Contraproposta.** Hoje a proposta é aceita ou recusada. Adicionar rodada
      de negociação com histórico:
  ```
  proposta_revisoes
   ├ proposta_id, autor_id
   ├ valor_proposto, mensagem
   ├ criado_em
   └ status  (ATIVA | SUPERADA | ACEITA | RECUSADA)
  ```
  Limite de rodadas para não virar leilão infinito.
- [ ] **Orçamento estruturado:** itens com descrição, quantidade, valor unitário.
      Melhor que valor único para serviço com material.
- [ ] **Agenda de disponibilidade** do trabalhador: janelas recorrentes por dia da
      semana + exceções (férias, feriado).
- [ ] **Agendamento** do contrato: data e hora combinadas, com verificação de
      conflito. Fuso horário sempre explícito — armazenar em UTC.
- [ ] **Cancelamento com motivo** e política: quem cancelou, em que estado, e o
      impacto na `taxa_conclusao`. Cancelar após `EM_ANDAMENTO` pesa mais que
      antes de iniciar.
- [ ] Expiração automática de proposta sem resposta (ex.: 7 dias) via `@Scheduled`.

**Aceite:** cliente e trabalhador negociam valor em rodadas, fecham com data
agendada, e o cancelamento reflete corretamente na reputação.

---

## Fase 13 — Notificações

**Depende de:** Fases 6, 7 e 10 (os eventos precisam existir)

- [ ] Tabela `notificacoes`: destinatário, tipo, título, corpo, `entidade_id`,
      `lida_em`, `criado_em`.
- [ ] Eventos cobertos:
  ```
  proposta recebida / respondida / expirando
  contrato  aceito → em andamento → concluído → cancelado
  nova mensagem (agrupada, não uma por mensagem)
  conexão solicitada / aceita
  avaliação recebida / publicada
  documento aprovado / rejeitado
  alerta de filtro salvo
  ```
- [ ] **Canal in-app:** entrega por WebSocket para quem está online; persistida
      para quem não está. Contador de não lidas.
- [ ] **Canal e-mail:** `spring-boot-starter-mail` + template Thymeleaf. Enviado
      de forma assíncrona (`@Async` com pool dedicado) — envio de e-mail nunca
      bloqueia a requisição HTTP nem participa da transação.
- [ ] **Preferências por evento e por canal**, com opt-out. Requisito de LGPD para
      comunicação não essencial.
- [ ] Agrupamento e janela de silêncio: 20 mensagens não geram 20 e-mails.
- [ ] Publicação por `ApplicationEventPublisher` + `@TransactionalEventListener(AFTER_COMMIT)`.
      Notificar antes do commit gera aviso de coisa que não aconteceu.
- [ ] Link de descadastro em todo e-mail não transacional.

**Aceite:** aceitar uma proposta gera notificação in-app instantânea e um e-mail;
o usuário consegue desligar o e-mail e continuar recebendo in-app.

---

## Fase 14 — Chat rico

**Depende de:** Fases 7 (chat funcional) e 4 (storage, para anexo)

- [ ] Lista de conversas ordenada por última atividade, com prévia da última
      mensagem e contador de não lidas.
- [ ] Confirmação de leitura (`mensagemLida` já existe na entidade e nunca foi
      usada) e indicador de entregue.
- [ ] Indicador de "digitando" via STOMP (evento efêmero, não persistido).
- [ ] Presença online / visto por último — com opção de desativar na privacidade.
- [ ] **Anexo no chat:** foto do problema, orçamento em PDF. Passa pelo
      `StorageService` e pelas validações da Fase 15.
- [ ] Busca dentro do histórico da conversa.
- [ ] Paginação por cursor no histórico (carregar mensagens antigas ao rolar).
- [ ] Chat vinculado ao contrato mostra o contexto no cabeçalho (serviço, valor,
      status).

**Aceite:** conversa com 5.000 mensagens carrega instantaneamente e rola sem
travar; anexo enviado e baixado pelos dois lados.

---

## Fase 15 — Anti-abuso e upload blindado

**Depende de:** Fases 3 (auth) e 4 (storage)
**Nota:** o rate limiting do login idealmente entra junto com a Fase 3. O resto
pode vir depois.

### 15.1 Rate limiting e força bruta

- [ ] Bucket4j com limites por IP e por usuário.
- [ ] Login: lockout progressivo (1s, 2s, 4s… após falhas consecutivas) e
      bloqueio temporário da conta, com notificação ao titular.
- [ ] Limites mais rígidos em: login, registro, recuperação de senha, envio de
      OTP, upload, criação de proposta.
- [ ] Resposta `429` com `Retry-After`.

### 15.2 Moderação

- [ ] Denúncia de usuário, serviço, post ou mensagem, com motivo.
- [ ] Fila de moderação e ações: advertência, remoção de conteúdo, suspensão,
      banimento.
- [ ] `usuarios.suspenso_ate` bloqueando ações de escrita.
- [ ] Detecção de padrão de spam: mesma mensagem para muitos destinatários,
      contato externo repetido, criação em massa de serviços.

### 15.3 Sanitização

- [ ] Todo conteúdo gerado por usuário passa por sanitização (OWASP Java HTML
      Sanitizer) antes de persistir: post, mensagem, comentário de avaliação,
      descrição de serviço, bio.
- [ ] Escapar na saída também — defesa em profundidade, o frontend pode falhar.

### 15.4 Upload blindado

Vai além do path traversal já corrigido na Fase 4.

- [ ] **Validação por magic bytes**, não por extensão nem por `Content-Type` —
      ambos são triviais de forjar. Usar Apache Tika para detectar o tipo real.
- [ ] Allowlist estrita por finalidade: imagem (JPEG/PNG/WebP) para post e
      portfólio; imagem ou PDF para certificado e documento.
- [ ] Reprocessar imagem (decodificar e recodificar) — remove payload embutido e
      metadados EXIF, incluindo **coordenadas GPS**, que são PII.
- [ ] Scan antivírus (ClamAV) com quarentena até liberar.
- [ ] Limite de tamanho e de quantidade por requisição e por usuário por dia.
- [ ] **URL assinada com expiração** para download, em vez de path adivinhável.
      Documento de identidade jamais em URL pública.

**Aceite:** arquivo `.jpg` que na verdade é executável é rejeitado; foto enviada
não carrega mais a localização GPS de quem tirou; 20 tentativas de login em
sequência resultam em bloqueio.

---

## Migrations — ordem consolidada

| Arquivo | Fase | Conteúdo |
|---|---|---|
| `V1__init.sql` | 2 | Schema base: users, worker_profiles, posts, connection, chats, messages |
| `V2__refresh_tokens.sql` | 3 | Refresh tokens revogáveis |
| `V3__dominio_servicos.sql` | 6 | servicos, propostas, contratos, avaliacoes |
| `V4__chat_contrato.sql` | 7 | `contrato_id` em chats |
| `V5__lgpd.sql` | 9 | consentimentos, audit_log, colunas de anonimização |
| `V6__verificacao_reputacao.sql` | 10 | verificações, métricas, avaliação bidirecional, portfólio |
| `V7__busca.sql` | 11 | categorias, colunas geográficas, índices GIN/GIST |
| `V8__negociacao_agenda.sql` | 12 | revisões de proposta, orçamento, disponibilidade |
| `V9__notificacoes.sql` | 13 | notificações e preferências |
| `V10__moderacao.sql` | 15 | denúncias, suspensões |

Migration já aplicada é **imutável**. Precisa mudar? Crie a próxima.

---

## Ordem de execução

```
PARTE I — fazer funcionar
  Fase 0  Preparação                ─┐
  Fase 1  Modelo unificado           ├─ app volta a subir
  Fase 2  Flyway V1                 ─┘
  Fase 3  Auth JWT + CORS           ── fecha os buracos de segurança
  Fase 4  Storage seguro
  Fase 5  Posts + Connection        ── base social funcional
  Fase 6  Domínio bico              ── núcleo do produto
  Fase 7  Chat
  Fase 8  Qualidade

PARTE II — produto e proteção de dados
  Fase 9   LGPD e PII               ← dep. 3
  Fase 10  Verificação + reputação  ← dep. 6, 9
  Fase 11  Descoberta e busca       ← dep. 6
  Fase 12  Negociação e agenda      ← dep. 6
  Fase 13  Notificações             ← dep. 6, 7, 10
  Fase 14  Chat rico                ← dep. 4, 7
  Fase 15  Anti-abuso + upload      ← dep. 3, 4
```

**Parte I.** Fases 0–2 são pré-requisito de tudo. Fase 3 antes de qualquer deploy
exposto. Fases 4 e 5 são independentes entre si. Fase 7 depende da 3 (Principal
no WebSocket) e da 6 (contrato como caminho de abertura de chat).

**Parte II.** Depois da Fase 9, as fases 11, 12 e 15 não dependem umas das outras
e podem ser reordenadas por prioridade. A 13 é a que mais depende de coisa pronta
— deixe por último dentro do grupo.

Ordem sugerida se for preciso escolher: **9 → 10 → 15 → 11 → 12 → 13 → 14**.
LGPD e reputação primeiro porque são, respectivamente, obrigação legal e o que
faz o produto funcionar. Anti-abuso logo depois, porque vale mais antes de ter
usuário real do que depois do primeiro incidente.

### Marcos

| Marco | Ao fim da | Significa |
|---|---|---|
| Sobe | Fase 2 | O erro de bootstrap JPA acabou |
| Seguro para expor | Fase 3 | Autenticação e autorização funcionando |
| Produto mínimo | Fase 7 | Fluxo completo publicar → contratar → conversar |
| Conforme | Fase 9 | Tratamento de dado pessoal em ordem |
| Pronto para usuário real | Fase 15 | Confiança, descoberta e anti-abuso no lugar |

---

## Fora de escopo

Decidido não entrar agora, registrado para depois.

**Autenticação** — decidido manter o mínimo (refresh + logout, na Fase 3):

- 2FA por TOTP
- Lista de sessões e dispositivos ativos, com encerramento remoto
- Política de senha com verificação contra vazamentos (HIBP k-anonymity)

**Infra e pipeline** — decidido incluir só CORS e headers (Fase 3):

- CI com build, teste e lint automatizados
- OWASP Dependency-Check e SAST no pipeline
- Secrets manager (Vault / AWS Secrets Manager) — por ora, variável de ambiente
- Backup automatizado com restore testado
- Docker multi-stage e compose completo da stack

**Produto:**

- Pagamentos, split e escrow
- Push mobile (FCM) — só faz sentido com app mobile no roadmap
- Internacionalização
- Painel administrativo (a moderação da Fase 15 nasce via API)
- S3/MinIO — a interface `StorageService` da Fase 4 deixa a troca pronta

### Riscos aceitos conscientemente

| Risco | Mitigação atual |
|---|---|
| Sem 2FA | Refresh token revogável + rate limiting no login (Fase 15) |
| Sem CI | Disciplina manual: `AGENTS.md` exige teste junto do código |
| Sem SAST/scan de dependência | Revisão manual; reavaliar antes de produção |
| Sem backup testado | **Bloqueia produção.** Resolver antes do primeiro usuário real |
