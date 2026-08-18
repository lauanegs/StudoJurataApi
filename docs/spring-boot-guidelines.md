# Spring Boot Guidelines — StudoJurataApi

Spring Boot 3.3.4, Java 21, Maven. Stack: `spring-boot-starter-web`, `-data-jpa`, `-security`, `-validation`, PostgreSQL (driver `org.postgresql`), Lombok, `springdoc-openapi` 2.6.0 (Swagger UI). Sem Flyway/Liquibase, sem Redis, sem mensageria, sem cache (`spring-boot-starter-cache`) — nada disso está no `pom.xml`, não presumir que existe.

## Controllers

- `@RestController` + `@RequestMapping("/recurso-no-plural-kebab-ou-simples")` (padrão real: `/simulados`, `/simulado-questao`, `/simulado-aluno` — kebab-case quando o nome é composto).
- Injeção por construtor (`@RequiredArgsConstructor`), nunca `@Autowired` em campo.
- Métodos finos: um controller não deve conter `if`/lógica condicional de negócio — isso já é verdade em ~30 dos 31 controllers do projeto; ao adicionar um endpoint novo, resista à tentação de validar algo "rápido" no controller. Validação de forma → `@Valid` no DTO; validação de regra → service.
- Sub-rotas de ação (não-CRUD) usam verbo no path: `POST /simulados/{id}/lancar`, `POST /simulados/{id}/encerrar`, `POST /simulado-aluno/{id}/finalizar`. Seguir esse padrão para novas ações de domínio, em vez de forçá-las em `PUT`/`PATCH` genérico.
- `@Valid @RequestBody` em todo DTO de entrada que tiver Bean Validation — mas hoje isso só está presente em 6 dos 17 DTOs (ver `docs/architecture.md` #4); ao tocar um endpoint sem validação, é uma boa oportunidade de adicionar (mas isso muda o comportamento de erro de 500/NPE para 400 — sinalizar antes se o endpoint já está em uso pelo front).

## Services / "Use Cases"

O projeto **não tem uma camada de Use Case separada do Service** (não há pacote `usecase/` nem uma convenção `XxxUseCase`). `@Service` concreto por entidade/agregado é o nível de granularidade real. Não introduzir uma camada de Use Case adicional entre Controller e Service sem uma razão concreta (ex.: uma orquestração que hoje não tem dono claro e passou a ser chamada de 3+ controllers diferentes) — para o tamanho e a maturidade atuais do projeto, isso seria uma camada extra sem ganho.

- `@Transactional` só nos métodos que precisam de atomicidade (múltiplas escritas relacionadas ou leitura+escrita que não pode ficar inconsistente). Não anotar a classe inteira nem métodos de leitura simples.
- Um service pode depender de outro service como colaborador (`NotaService` depende de `NotificacaoService` e `AuditLogService`) — isso é aceito e é o mecanismo do projeto para orquestrar efeitos colaterais. Evitar dependência circular entre services (A depende de B que depende de A) — nenhuma existe hoje; ao adicionar uma nova dependência entre services, checar isso.

## Repositories

- Interface `JpaRepository<Entity, Long>`. Query methods derivados por nome são o padrão esmagador (`findByAluno_IdAndStatus`, `existsBySimuladoIdAndAlunoId`). Use `@Query` (JPQL) só quando o nome do método ficaria absurdamente longo ou a query precisa de `JOIN FETCH`/agregação que o nome não expressa bem — hoje só há 1 caso no projeto inteiro (`AulaRepository`), o que indica que a barra para usar `@Query` deve continuar alta.
- Nome de propriedade aninhada usa `_` explícito quando há ambiguidade (`findByAluno_IdAndStatus`) — seguir esse estilo em vez de contar com a resolução implícita do Spring Data quando o nome da propriedade tiver múltiplas interpretações possíveis.
- **Sem paginação hoje.** Se um endpoint novo precisar listar uma tabela que pode crescer muito (ex.: `AuditLog`, `Nota`), considerar `Pageable` desde o início nesse endpoint específico — mas isso é uma mudança de contrato de resposta (a API muda de `List<T>` para `Page<T>`/objeto com metadados), então sinalizar antes de aplicar em um endpoint que o front já consome, e não fazer "de brinde" em um PR sobre outra coisa.

## Entities

- Sempre estender `BaseEntity` (id `IDENTITY` + auditoria automática de `createdAt`/`updatedAt`). Não reimplementar isso por entidade.
- Relacionamentos: `@ManyToOne`/`@OneToOne`/`@OneToMany` sem `fetch` explícito na maioria dos casos (usa o padrão do JPA: `EAGER` para `@ManyToOne`/`@OneToOne`, `LAZY` para `@OneToMany`/`@ManyToMany`) — o projeto não força `LAZY` em tudo. Se um `@ManyToOne` específico começar a causar N+1 perceptível, resolver com `@Query`+`JOIN FETCH` nesse ponto específico, não mudar o fetch padrão do projeto inteiro de uma vez.
- `@Column(nullable = false)`/`unique = true` são usados quando a regra é realmente uma invariante de banco (`Usuario.username` único, `Usuario.escola` obrigatório) — continuar usando constraints de banco para invariantes que não podem depender só da validação da aplicação.
- **Cuidado ao remover ou tornar `NOT NULL` um campo de entidade já populada.** `ddl-auto=update` nunca remove/relaxa colunas: remover um campo Java deixa a coluna órfã (`NOT NULL` órfã já quebrou o `DevDataResetSeeder` uma vez, documentado no histórico do projeto). Ao remover um `@Column`, draftar o `ALTER TABLE ... DROP COLUMN` correspondente junto da mudança. Ao adicionar um `@Column(nullable = false)` novo numa tabela já populada, `ddl-auto=update` vai falhar tentando `ADD COLUMN ... NOT NULL` em linhas existentes — ou adicionar com um valor default, ou nascer `nullable = true` e reforçar em aplicação.

## DTOs

- Um `RequestDTO` e um `ResponseDTO` por entidade principal — nunca serializar a entidade JPA diretamente na resposta (evita expor relações lazy, campos sensíveis, e acopla o contrato de API ao schema do banco).
- Campo sensível que precisa ser **aceito** mas nunca **devolvido**: `@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)` — não `@JsonIgnore` (bloqueia as duas direções; já foi um bug real do projeto em `Usuario.senha`, que quebrava todo `POST /usuarios`).

## Mapeadores

- `@Component` manual, método `toEntity`/`toResponseDTO`. Sem MapStruct/ModelMapper no classpath — não introduzir uma lib de mapeamento sem justificar o ganho sobre o padrão manual atual (o projeto tem ~15 mappers manuais consistentes; migrar todos para MapStruct é uma mudança grande de tooling, não uma correção pontual).
- Mapper pode injetar repositórios para resolver um ID recebido em relação completa — e deve lançar `RecursoNaoEncontradoException` quando o ID não existir, em vez de deixar `null`/`NullPointerException` silencioso.

## Validações

Ver `docs/architecture.md`/`docs/clean-code.md`. Regra prática: Bean Validation (`@NotBlank`, `@NotNull`, `@Size`, etc.) no DTO para forma; `RegraNegocioException`/`RequisicaoInvalidaException` no service para regra dependente de estado. `MethodArgumentNotValidException` já é tratada centralmente no `GlobalExceptionHandler`, agregando todos os erros de campo numa mensagem só — não capturar essa exceção manualmente em um controller específico.

## Transações

- `@Transactional` de método (não de classe), só onde há mais de uma escrita relacionada ou uma invariante que não pode ficar visível pela metade (ex.: `SimuladoService.lancar()`, `NotaService.recalcular()`).
- Não anotar métodos `@Transactional` que só fazem uma leitura (`findAll()`, `findById()`) — não agrega nada e sinaliza intenção errada para quem lê o código depois.

## Exceptions / Responses

Ver `docs/clean-code.md`. Toda resposta de erro passa por `GlobalExceptionHandler` → `ErrorResponse` padronizado. Novo tipo de erro de negócio → nova subclasse de `RuntimeException` + handler dedicado, nunca `try/catch` local devolvendo um `ResponseEntity` ad-hoc dentro do controller.

## Configuração

- Um único `application.properties` (sem profiles `dev`/`prod` além do `seed` usado só pelo `DevDataResetSeeder`). Configuração sensível hoje está em texto puro no arquivo versionado (usuário/senha do Postgres) — aceitável para ambiente 100% local de desenvolvimento como está hoje, mas **não deve ir para produção sem migrar para variável de ambiente/secret manager**; sinalizar isso explicitamente se a tarefa envolver preparar deploy.
- Propriedades de feature específicas (Gemini) usam `@Value("${studojurata.ia.gemini.api-key:}")` com valor default vazio/seguro, nunca falhando o boot da aplicação por falta de config — só falha (com exceção de domínio, `GeminiIndisponivelException`) no momento em que a feature é efetivamente usada. Seguir esse padrão para qualquer integração externa nova e opcional.

## Injeção de dependências

Construtor via `@RequiredArgsConstructor` em 100% do código-fonte inspecionado. Não introduzir `@Autowired` em campo nem setter injection.

## JPA/Hibernate

- `spring.jpa.hibernate.ddl-auto=update`, `show-sql=true`, `format_sql=true` — só para dev. Se/quando o projeto migrar para uma ferramenta de migration (Flyway/Liquibase), isso é uma decisão de infraestrutura relevante o bastante para ser tratada como tarefa própria, não um efeito colateral de outra mudança.
- `@Enumerated(EnumType.STRING)` sempre, nunca `ORDINAL`.
- `equals`/`hashCode` de entidade só pelo `id` (via `BaseEntity`) — nunca `@Data` em `@Entity`.

## Queries

Query methods do Spring Data como primeira opção. `@Query` (JPQL) reservada para casos que o nome do método não expressa bem. Sem SQL nativo (`nativeQuery = true`) em nenhum ponto do projeto hoje — introduzir isso é uma decisão de portabilidade a justificar, não um atalho para "resolver mais rápido".

## Paginação

Não implementada hoje em nenhum endpoint (ver `docs/architecture.md` #2). Ao introduzir, usar `Pageable`/`Page<T>` do Spring Data, expor `page`/`size`/`sort` como query params — e tratar como mudança de contrato de API a ser sinalizada, não uma "melhoria silenciosa" no meio de outra tarefa.

## Relacionamentos

`@ManyToOne`/`@OneToOne`/`@OneToMany` mapeados diretamente, sem uso de `@ManyToMany` direto em nenhuma entidade hoje (relações N:N passam por entidade de associação explícita, como `AlunoTurma`, `SimuladoQuestao`, `QuestaoConteudo` — cada uma como uma entidade própria com seus próprios atributos/status). **Preferir esse padrão** (entidade de associação explícita) a introduzir `@ManyToMany` puro quando a relação também carrega estado (status, data, pontuação) — é exatamente o caso do projeto hoje.

## Performance

Sem cache, sem paginação, sem índices customizados documentados além dos implícitos de `unique = true`/chave estrangeira. Para o volume de dados de demonstração/MVP atual isso não é um problema observável — não otimizar preventivamente sem um sintoma real (query lenta medida, endpoint sabidamente usado com volume alto). Se/quando otimizar, preferir resolver no nível mais baixo possível (índice, query method) antes de introduzir cache de aplicação.

## Segurança

Ver `docs/architecture.md` seção 4 para o estado real. Regras para manter/estender:
- Toda nova rota de escrita (`POST`/`PUT`/`DELETE`) precisa de uma entrada explícita em `SecurityConfig` — não confiar no `anyRequest().authenticated()` genérico do final da cadeia para decidir o papel certo; ele é o fallback, não a regra.
- Dado pessoal de Aluno (notas, gamificação, respostas de simulado) exige checagem de dono via guard (`AlunoAccessGuard` ou equivalente) além do papel — papel sozinho (`hasRole("ALUNO")`) não impede um Aluno de acessar dado de outro Aluno trocando um ID na URL.
- Senha sempre BCrypt via o `PasswordEncoder` bean existente — nunca hash manual, nunca texto puro.
- Multi-tenant (`EscolaContext`) hoje é parcial — ao adicionar um novo endpoint de listagem para uma entidade que pertence a uma Escola (direta ou indiretamente), avaliar se ele deveria filtrar por `EscolaContext.escolaAtualId()` como os 5 services que já fazem isso, em vez de assumir que não filtrar é o padrão certo só porque a maioria não filtra hoje.

## Referências
- [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/docs/3.3.4/reference/html/) (versão correspondente ao `pom.xml`).
- [Spring Data JPA Reference](https://docs.spring.io/spring-data/jpa/reference/) — query methods, paginação.
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/) — `SecurityFilterChain`, sessão vs. token.
- [Baeldung — Spring Boot Best Practices](https://www.baeldung.com/spring-boot) (síntese de padrões comuns da comunidade, usada como referência geral, não copiada).
