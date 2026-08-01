# Orientações para agentes de IA — Severinus

Leia este arquivo antes de escrever qualquer código neste repositório.

## O projeto

Severinus é uma API de marketplace de serviços informais ("bicos"). Trabalhadores
publicam serviços, clientes enviam propostas, o aceite vira contrato, e as partes
conversam por chat em tempo real. Mistura rede profissional (perfil, feed,
conexões) com contratação sob demanda.

**Stack:** Java 21 · Spring Boot 3.4.2 · Spring Data JPA / Hibernate 6 ·
Spring Security · WebSocket STOMP · PostgreSQL · Flyway · Lombok · Maven.

## Estado atual

> **A aplicação não sobe.** Há erros de mapeamento JPA em aberto. O plano de
> correção, com inventário de bugs por `arquivo:linha` e fases de execução, está
> em [`docs/PLANO.md`](docs/PLANO.md). **Consulte esse documento antes de propor
> mudanças** — muito do que parece um bug isolado já está mapeado lá, e a ordem
> das fases importa.

## Comandos

```bash
docker compose up -d          # Postgres em localhost:5434
./mvnw -o compile             # compilar
./mvnw -o test                # rodar testes
./mvnw -o spring-boot:run     # subir a API em :8085
```

Prefira `-o` (offline) — o repositório local já tem as dependências.

---

# Regras obrigatórias

As três regras a seguir são **inegociáveis**. Código que não as cumpre não está
pronto, independentemente de compilar e passar nos testes.

## 1. Testes unitários da camada de service

Todo método público de `service` precisa de teste unitário. JUnit 5 + Mockito,
repositórios mockados, sem subir contexto Spring.

Cada método testado cobre **caminho feliz e casos de erro/borda**. Um teste só do
caminho feliz não cumpre a regra.

```java
@ExtendWith(MockitoExtension.class)
class PropostaServiceTest {

    @Mock private PropostaRepository propostaRepository;
    @InjectMocks private PropostaService propostaService;

    @Test
    void deveAceitarPropostaPendente() { ... }

    @Test
    void deveRejeitarPropostaJaAceita() { ... }

    @Test
    void deveLancarQuandoPropostaNaoEncontrada() { ... }
}
```

- Arquivo espelha o de produção: `PropostaService` → `PropostaServiceTest`.
- Nome do teste descreve o comportamento em português:
  `deveLancarQuandoUsuarioNaoEhDono`, não `test1` nem `testAceitar`.
- Estrutura **arrange / act / assert** explícita.
- Verifique interações relevantes com `verify(...)`, não só o retorno.
- Um cenário por teste. Sem `@Test` com cinco asserts sobre coisas diferentes.
- **Ao alterar um service existente, atualize o teste dele na mesma mudança.**

Testes de integração (com Testcontainers) são bem-vindos, mas **não substituem**
o teste unitário do service.

## 2. Documentação Swagger em todos os controllers

O projeto usa `springdoc-openapi`. Todo controller e todo método exposto precisa
estar documentado. UI em `/swagger-ui.html`.

```java
@Tag(name = "Propostas", description = "Envio e resposta de propostas de serviço")
@RestController
@RequestMapping("/propostas")
public class PropostaController {

    @Operation(
        summary = "Aceita uma proposta",
        description = "Aceita uma proposta pendente e gera o contrato correspondente."
    )
    @ApiResponse(responseCode = "200", description = "Proposta aceita")
    @ApiResponse(responseCode = "403", description = "Usuário não é o dono do serviço")
    @ApiResponse(responseCode = "404", description = "Proposta não encontrada")
    @ApiResponse(responseCode = "409", description = "Proposta não está pendente")
    @PatchMapping("/{id}/aceitar")
    public ResponseEntity<PropostaResponseDto> aceitar(@PathVariable UUID id) { ... }
}
```

Regras:

- `@Tag` em todo controller.
- `@Operation` com `summary` em todo método.
- `@ApiResponse` para **todos os códigos que o método realmente retorna** —
  incluindo 400, 403, 404 e 409. Documentar só o 200 não cumpre a regra.
- `@Schema` nos campos dos DTOs, com `description` e `example`.
- Textos em português.
- Endpoints protegidos marcados com `@SecurityRequirement(name = "bearerAuth")`.

## 3. Javadoc em português

Toda classe e todo método público levam Javadoc, escrito em **português**.

```java
/**
 * Regras de negócio das propostas enviadas a um serviço.
 *
 * <p>Uma proposta nasce {@code PENDENTE} e só pode ser respondida pelo
 * trabalhador dono do serviço. O aceite gera o contrato correspondente.
 */
@Service
public class PropostaService {

    /**
     * Aceita uma proposta pendente e cria o contrato correspondente.
     *
     * @param propostaId identificador da proposta
     * @param usuarioId  identificador do usuário autenticado, que precisa ser o
     *                   dono do serviço
     * @return o contrato criado
     * @throws EntityNotFoundException se a proposta não existir
     * @throws AccessDeniedException   se o usuário não for o dono do serviço
     * @throws EstadoInvalidoException se a proposta não estiver pendente
     */
    public Contrato aceitar(UUID propostaId, UUID usuarioId) { ... }
}
```

Regras:

- Classes: o que a classe faz e qual sua responsabilidade — não repetir o nome.
- Métodos públicos: `@param` para cada parâmetro, `@return` quando não for `void`,
  `@throws` para cada exceção lançada intencionalmente.
- Descreva **o porquê e as regras**, não o óbvio.
  `/** Retorna o id. */` sobre `getId()` é ruído — omita.
- Entidades: documente as invariantes e as relações.
- Enums: documente cada constante quando o significado não for evidente.

## 4. Documentação acompanha o código

Toda alteração ou adição de código atualiza, na **mesma mudança**, a documentação
afetada. Código novo com documentação desatualizada é entrega incompleta.

Ao mexer em algo, verifique e atualize o que for aplicável:

| Você mudou | Atualize também |
|---|---|
| Endpoint (criou, removeu, mudou rota/parâmetro/resposta) | `@Operation` / `@ApiResponse` do método e o `@Tag` do controller |
| Assinatura de método público | Javadoc: `@param`, `@return`, `@throws` |
| Campo de DTO | `@Schema` do campo (`description`, `example`) e o Javadoc da classe |
| Entidade ou schema | Migration Flyway + Javadoc das invariantes e relações |
| Regra de negócio | Javadoc do service explicando a regra nova |
| Fase concluída, decisão revista ou item novo | `docs/PLANO.md` |
| Setup, comando, variável de ambiente, dependência | `README.md` e `.env.example` |
| Convenção ou padrão do projeto | este `AGENTS.md` |

Regras:

- Nunca deixe documentação descrevendo comportamento que não existe mais.
  Documentação errada é pior que documentação ausente.
- Removeu código? Remova a documentação dele junto.
- Se a mudança altera a forma como o cliente da API consome o endpoint, isso
  **precisa** aparecer no Swagger antes de a tarefa ser considerada concluída.
- `docs/PLANO.md` é documento de decisão: marque itens concluídos e registre
  desvios, mas **não reescreva decisões sem alinhar antes**.

---

# Boas práticas

## Arquitetura em camadas

```
Controller  →  Service  →  Repository
  fino        regra de     acesso a
              negócio      dados
```

- **Controller nunca acessa Repository direto.** Sempre via service.
- Controller só faz: receber, validar (`@Valid`), delegar, montar a resposta HTTP.
  Sem `if` de regra de negócio, sem cálculo, sem acesso a `Files`/`Paths`.
- Regra de negócio mora no **service**. Não em controller, não em entidade,
  não em repository.
- Repository só declara consultas. Sem lógica.
- `@Transactional` no service, não no controller.
- Service não conhece tipos web (`ResponseEntity`, `MultipartFile` na assinatura,
  `HttpServletRequest`). Converta na borda.

## DTOs — nunca exponha a entidade

Entidade JPA **jamais** entra ou sai de um controller.

- **Entrada:** `CreateXDto` / `UpdateXDto` com `@Valid` e Bean Validation
  (`@NotBlank`, `@Email`, `@Size`, `@Positive`).
- **Saída:** `XResponseDto` sem campo sensível. **Senha e hash de senha nunca
  saem da API.** CPF/CNPJ só para o próprio dono.
- Isso já é um bug real no projeto: `GET /user/users` devolve a entidade crua com
  a senha em texto puro. Não repita o padrão.

## JPA e Hibernate

Esta seção existe porque erros dessa classe já derrubaram a aplicação.

- **`@ManyToOne`/`@OneToOne` apontam para `@Entity`, nunca para `UUID`.**
  `@ManyToOne UserEntity autor`, não `@ManyToOne UUID autorId`. Isso quebra o
  bootstrap do Hibernate.
- **Não use `@Data` em entidade.** Gera `equals`/`hashCode` sobre todos os campos,
  incluindo coleções LAZY — causa `LazyInitializationException` e recursão
  infinita em relações bidirecionais. Use `@Getter`/`@Setter` e implemente
  `equals`/`hashCode` sobre o `id`.
- **Todo `@ManyToOne` com `fetch = FetchType.LAZY`.** O padrão é EAGER e gera N+1.
- **`List<String>` em entidade exige `@ElementCollection`.** Sem isso, não persiste.
- Coleções inicializadas (`@Builder.Default private List<X> itens = new ArrayList<>()`)
  para nunca serem `null`.
- `@JoinColumn` com nome **distinto** por associação. Dois campos com o mesmo
  nome de coluna quebram o mapeamento.
- Sem acento ou caractere especial em nome de campo Java.
- Ao criar consulta derivada (`findByAlgumaCoisa`), confirme que o nome do campo
  existe na entidade. Erro aqui só aparece no boot, não na compilação.
- Listagens sempre com `Pageable`. Nada de `findAll()` sem paginação.
- Atenção a N+1: use `@EntityGraph` ou `join fetch` quando percorrer relações.

## Segurança

- **Senha só com BCrypt.** Nunca em texto puro, nunca em log, nunca em resposta.
- **A identidade do usuário vem do token, nunca do path ou do body.** Rota como
  `POST /posts/{userId}` com o autor vindo do path permite postar em nome de
  terceiro. Use o usuário autenticado.
- Toda operação sobre recurso de terceiro checa autorização no service.
- Sem credencial hard-coded. Use variável de ambiente com default só para dev:
  `${DB_PASSWORD:admin}`.
- Upload: **nome de arquivo gerado no servidor** (UUID + extensão validada),
  nunca `getOriginalFilename()`. Valide que o path resolvido está dentro do
  diretório base — `cleanPath` não impede path traversal.
- Endpoint novo nasce autenticado. Liberar exige justificativa.
- Nunca logar token, senha ou dado pessoal.

## Migrations

- **Toda mudança de schema é uma migration Flyway.** `ddl-auto` fica em
  `validate` e não deve ser alterado.
- Migration é **imutável**: nunca edite um arquivo `V*.sql` já aplicado. Crie o
  próximo número.
- Nomenclatura: `V<n>__descricao_em_snake_case.sql`.
- Mudou entidade? A migration correspondente vai **na mesma alteração**.
- Índice para toda coluna usada em filtro ou ordenação frequente.

## Tratamento de erros

- Sem `throw new RuntimeException()` sem mensagem. Use exceção específica com
  texto útil.
- Exceções de domínio próprias (`EntityNotFoundException`, `AccessDeniedException`,
  `EstadoInvalidoException`), traduzidas para HTTP no `@ControllerAdvice` global.
- Controller não faz try/catch de regra de negócio.

## Estilo

- Código, nomes de classe e nomes de método em **português**, seguindo o padrão
  já existente (`criarUsuario`, `buscarSolicitacoes`). Termos técnicos consagrados
  ficam em inglês (`Repository`, `Service`, `Dto`, `findBy...`).
- Indentação de 4 espaços. Sem tabs. Sem mistura — o `pom.xml` atual tem
  indentação inconsistente; não replique.
- Injeção **por construtor**, não `@Autowired` em campo. Facilita teste e deixa a
  dependência obrigatória explícita.
- Sem import não usado, sem código morto, sem `System.out.println`.
- `@SuppressWarnings` só com motivo real e comentário explicando.
- Um arquivo por classe pública.

---

# Antes de considerar o trabalho pronto

- [ ] `./mvnw -o test` passa
- [ ] Service novo/alterado tem teste unitário cobrindo sucesso **e** erro
- [ ] Controller novo/alterado tem `@Tag`, `@Operation` e `@ApiResponse` completos
- [ ] Classes e métodos públicos com Javadoc em português
- [ ] Nenhuma entidade JPA cruzando a fronteira do controller
- [ ] Mudança de schema acompanhada de migration Flyway
- [ ] Nenhum segredo, senha ou dado pessoal em log ou resposta
- [ ] Identidade do usuário vindo do token, não do path/body
- [ ] Documentação afetada atualizada na mesma mudança (Swagger, Javadoc,
      `README.md`, `.env.example`, `docs/PLANO.md`)

## Ao entregar

- Reporte honestamente: teste que falhou, passo que ficou de fora, suposição que
  foi feita. Não afirme que está pronto se não estiver.
- Não commite nem faça push sem pedido explícito.
- Não altere `docs/PLANO.md` sem alinhar antes — é documento de decisão.
