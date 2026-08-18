# Arquitetura — StudoJurataApi

> Este documento descreve a arquitetura **real** do projeto, como ela existe hoje no código, não uma arquitetura ideal. Foi escrito a partir de inspeção direta do código-fonte (não de suposições). Última análise: 2026-08-10.

## 1. Visão geral

API REST monolítica em **Spring Boot 3.3.4 / Java 21**, para o backend do portal escolar Studo Jurata. Persistência em **PostgreSQL** via **Spring Data JPA/Hibernate**. Autenticação por **sessão de servidor** (cookie `JSESSIONID`, Spring Security), não por JWT/token. Um único módulo Maven, sem multi-módulo.

Não é uma arquitetura hexagonal, DDD ou "clean architecture" com portas/adaptadores. É uma **arquitetura em camadas (layered/N-tier) clássica**, com pacotes organizados majoritariamente **por tipo técnico** (`controller`, `service`, `repository`, `model`, `dto`, `mapper`), e uma exceção: o pacote `ia` é organizado **por feature** (tem seus próprios `controller/dto/mapper/model/repository/service` internos). Essa mistura é intencional o suficiente para não ser "corrigida" sem necessidade — ver seção 6.

## 2. Camadas e responsabilidades

```
HTTP request
   │
   ▼
Controller (@RestController)      — mapeia rota HTTP ↔ DTO. Sem lógica de negócio.
   │  usa @Valid nos DTOs de entrada
   ▼
Mapper (@Component)               — converte DTO ↔ Entity. Pode consultar repositórios
   │                                para resolver relacionamentos (ex.: disciplinaId → Disciplina).
   ▼
Service (@Service)                — regra de negócio, validações de domínio, orquestração
   │                                entre repositórios, @Transactional quando necessário.
   ▼
Repository (interface JpaRepository) — acesso a dados. Sem lógica de negócio.
   │
   ▼
Entity (@Entity extends BaseEntity)  — mapeamento JPA. Getters/Setters via Lombok.
   │
   ▼
PostgreSQL (schema gerenciado por ddl-auto=update)
```

### Controller
- Um `@RestController` por entidade principal (`SimuladoController`, `AlunoController`, etc.), quase sempre 1:1 com o Service correspondente.
- Método padrão: `listar()`, `buscar(id)`, `salvar(dto)`, `atualizar(id, dto)`, `deletar(id)` — nomenclatura em português, consistente em ~30 controllers.
- Controllers são **finos de propósito**: delegam para `mapper.toEntity()` → `service.<ação>()` → `mapper.toResponseDTO()`, tudo em uma linha na maioria dos métodos. Confirmado por inspeção: praticamente nenhum controller tem `if`/lógica condicional própria — é convenção real do projeto, não coincidência.
- Autorização é declarada centralizadamente em `SecurityConfig` (`.requestMatchers(...)`), não em anotações `@PreAuthorize` nos controllers. Quando a regra depende do dono do recurso (não só do papel), o controller/service chama `AlunoAccessGuard.garantir(alunoId)` explicitamente (ver seção 4).
- Injeção de dependência sempre por **construtor**, via `@RequiredArgsConstructor` (Lombok) em campos `private final`. Não há `@Autowired` em campo em nenhum arquivo inspecionado.

### Mapper
- Classes `@Component` manuais (**não** há MapStruct nem outra lib de mapeamento no `pom.xml`). Cada mapper tem `toEntity(RequestDTO)` e `toResponseDTO(Entity)`.
- Quando o DTO de entrada só carrega um ID de relacionamento (ex.: `disciplinaId`), o mapper busca a entidade completa via repositório e lança `RecursoNaoEncontradoException` se não existir — ou seja, o mapper participa da validação de existência de FKs, não só da tradução de campos.
- Vive em pacote próprio (`mapper/`), separado do `service/`.

### Service
- `@Service` concreto, **sem interface** (`XxxService`, nunca `XxxServiceImpl implements XxxService`). Isso é uma convenção deliberada do projeto — não introduzir interfaces de serviço sem uma razão concreta (múltiplas implementações reais, mock em teste que precise disso, etc.).
- Contém toda a regra de negócio: transições de status (`Simulado.status: RASCUNHO → PUBLICADO → ENCERRADO`), validações que dependem de estado do banco, orquestração entre múltiplos repositórios, disparo de notificações/auditoria.
- `@Transactional` é usado pontualmente (19 ocorrências) nos métodos que fazem múltiplas escritas relacionadas (ex.: `SimuladoService.lancar()` cria N `SimuladoAluno` e atualiza o `Simulado`) ou que precisam de atomicidade por regra de negócio (`NotaService.recalcular()`). Métodos de leitura e CRUD simples não são anotados — o projeto não usa `@Transactional` "por padrão" na classe inteira.
- Exceções de negócio são customizadas e sem relação com HTTP (`RecursoNaoEncontradoException`, `RegraNegocioException`, `RequisicaoInvalidaException`) — a tradução para status HTTP acontece só no `GlobalExceptionHandler`. Isso mantém os services livres de `import org.springframework.http.*`.

### Repository
- Interfaces `JpaRepository<Entity, Long>`, com métodos derivados por nome (`findByAluno_IdAndStatus(...)`) como padrão esmagador. `@Query` explícita aparece **uma única vez** em todo o projeto (`AulaRepository`) — o projeto prefere query methods do Spring Data a JPQL/SQL manual sempre que possível.
- Sem paginação: nenhum repositório usa `Pageable`/`Page<T>` em todo o código-fonte. Todo `listar()` retorna `List<T>` completo. Ver "Problemas arquiteturais identificados".

### Entity (`model/`)
- Todas estendem `BaseEntity` (`@MappedSuperclass`), que centraliza `id` (`IDENTITY`), `createdAt`/`updatedAt` via `@CreatedDate`/`@LastModifiedDate` (JPA Auditing, habilitado globalmente em `StudojurataApiApplication`).
- Padrão Lombok consistente: `@Getter @Setter @EqualsAndHashCode(callSuper = true)` na entidade, com `@EqualsAndHashCode.Include` só no `id` (definido em `BaseEntity`). **Nunca `@Data`** em entidade — busca deliberada por evitar `equals`/`hashCode`/`toString` gerados a partir de todos os campos (o que quebra em relações bidirecionais e em coleções lazy do Hibernate). Preservar esse padrão em qualquer entidade nova ou revisada.
- Enums de domínio (`StatusSimulado`, `TipoUsuario`, etc., 17 no total) vivem em `model/enums/`, mapeados com `@Enumerated(EnumType.STRING)` — nunca `ORDINAL`.

### DTO
- Um `RequestDTO`/`ResponseDTO` por entidade principal (ou próximo disso), em vez de reusar a entidade diretamente no contrato HTTP. `@Getter @Setter` puro (nunca a entidade JPA sai/entra direto no controller).
- Cobertura de validação **é inconsistente**: das 17 classes em `dto/`, só 6 usam `@NotNull`/`@NotBlank`/etc. Não é uma regra "toda entrada tem Bean Validation" ainda aplicada uniformemente — ver seção 6.

## 3. Fluxo de uma requisição (exemplo real: `POST /simulados/{id}/lancar`)

1. `SecurityFilterChain` (Spring Security) autentica pela sessão e checa `hasAnyRole("PROFESSOR","ADMINISTRADOR")` para `POST /simulados/**`.
2. `SimuladoController.lancar(id, LancarSimuladoRequest)` recebe a requisição, sem lógica própria além de extrair `alunoIds`.
3. `SimuladoService.lancar(id, alunoIds)`, dentro de `@Transactional`:
   - busca o `Simulado` (`RecursoNaoEncontradoException` se não existir);
   - valida status (`RegraNegocioException` se já lançado);
   - valida que há questões ativas e todas aprovadas (`RegraNegocioException`);
   - resolve a lista de alunos elegíveis (por turma ou por lista explícita, `RequisicaoInvalidaException` se faltar dado);
   - cria `SimuladoAluno` (status `PENDENTE`) por aluno elegível, idempotente (pula quem já foi convocado);
   - atualiza `Simulado.status = PUBLICADO`.
4. `SimuladoMapper.toResponseDTO()` converte a entidade atualizada em `SimuladoResponseDTO`.
5. Se qualquer exceção de negócio for lançada, `GlobalExceptionHandler` a traduz para o status HTTP e corpo `ErrorResponse` padronizados.

Esse fluxo — Controller fino → Mapper resolve relações → Service concentra regra de negócio e transação → Repository persiste — é o padrão a seguir em qualquer novo endpoint ou revisão.

## 4. Segurança — como realmente funciona hoje

- **Autenticação**: sessão de servidor (Spring Security + cookie), não stateless/JWT. `POST /auth/login` é o único endpoint público de negócio; Swagger também é público em dev.
- **Autorização por papel**: centralizada em `SecurityConfig.securityFilterChain()`, com uma lista extensa e comentada de `requestMatchers(...)` por método HTTP + papel (`ADMINISTRADOR`, `PROFESSOR`, `ALUNO`). Não usa `@PreAuthorize`/`@Secured` em métodos.
- **Autorização por dono do recurso (IDOR)**: quando um papel amplo (`authenticated()`) não é suficiente porque o dado é pessoal (notas, gamificação), o código usa `AlunoAccessGuard.garantir(alunoId)`, injetado no controller/service correspondente, que barra um Aluno tentando ler/alterar dados de outro Aluno. Professor/Administrador têm acesso irrestrito.
- **Multi-tenant (Escola)**: existe `EscolaContext.escolaAtualId()` para filtrar dados pela escola do usuário logado, mas **só é usado em 5 dos 30 services** (`Curso`, `Disciplina`, `PlanoEnsino`, `Turma`, `Usuario`). As demais entidades (`Aluno`, `Simulado`, `Nota`, etc.) não filtram por escola — ver "Problemas arquiteturais identificados".
- **Senha**: sempre hash BCrypt (`PasswordEncoder` bean), nunca texto puro. O campo `Usuario.senha` usa `@JsonProperty(access = WRITE_ONLY)` — aceita entrada via JSON, nunca é serializado na resposta (não usar `@JsonIgnore` aqui: bloqueia as duas direções, já foi um bug real do projeto).
- **CORS**: liberado por padrão de porta (`http://localhost:*`), com `allowCredentials(true)`, pensado para o Vite trocar de porta em dev. Precisa de origem de produção explícita antes de deploy real.
- **CSRF**: desabilitado (`csrf.disable()`) — aceitável para API consumida por SPA própria com CORS restrito, mas é uma decisão a manter consciente, não a reintroduzir "por padrão" nem remover sem entender o motivo.

## 5. Tratamento de erros

Centralizado em `GlobalExceptionHandler` (`@RestControllerAdvice`), que mapeia:

| Exceção | HTTP |
|---|---|
| `RecursoNaoEncontradoException` | 404 |
| `RegraNegocioException` | 409 |
| `RequisicaoInvalidaException` | 400 |
| `MethodArgumentNotValidException` (Bean Validation) | 400, com mensagem `campo: motivo` agregando todos os erros |
| `DataIntegrityViolationException` (FK/constraint do banco) | 409, mensagem genérica de "registro com dependentes" |
| `NoSuchElementException` | 404 (compatibilidade com `orElseThrow()` sem argumento em código legado) |
| `ResponseStatusException` | repassa o status original (usado por `AlunoAccessGuard`, que lança 403 diretamente) |

Todas as respostas de erro usam o mesmo formato (`ErrorResponse`: timestamp, status, error, message, path). Esse é o padrão a seguir: **nunca deixar uma exceção de negócio vazar como 500** — criar/reusar uma das três exceções de domínio, ou tratá-la explicitamente no handler.

## 6. Problemas arquiteturais identificados

Estes são registrados para orientar revisões futuras — **não devem ser corrigidos automaticamente** sem avaliação caso a caso, e alguns podem ser aceitáveis para o estágio atual do projeto (MVP).

1. **Isolamento multi-tenant parcial.** `EscolaContext` existe mas só é aplicado em 5 dos 30 services. Um Professor/Admin de uma escola pode, em tese, listar/acessar Alunos, Simulados, Notas etc. de outra escola via API. Se o projeto realmente pretende suportar múltiplas escolas isoladas, isso é uma lacuna de segurança relevante — mas exige entender a intenção do produto antes de "fechar" tudo (pode haver dados intencionalmente globais, ex.: catálogo de questões).
2. **Sem paginação em nenhum endpoint de listagem.** Todo `GET` de coleção (`/alunos`, `/questoes`, `/simulados`, etc.) devolve a tabela inteira. Funciona hoje com o volume de dados do seed/demonstração; é um risco de performance/memória quando o volume de dados crescer. Introduzir paginação é uma mudança de contrato de API (formato de resposta muda de array para objeto paginado) — sinalizar ao usuário antes de aplicar, não é "correção silenciosa".
3. **Cobertura de testes automatizados é essencialmente zero.** Existe um único teste (`StudojurataApiApplicationTests.contextLoads()`), sem testes de unidade de service, sem testes de integração de controller/repositório. Qualquer refatoração precisa compensar isso com testes manuais via API (scripts, Swagger, Postman) documentados no PR/sessão, já que não há rede de segurança automatizada.
4. **Validação de entrada (`Bean Validation`) inconsistente.** Só 6 das 17 classes em `dto/` usam `@NotNull`/`@NotBlank`. Os demais DTOs contam inteiramente com validação manual dentro do service (ou nem isso). Não uniformizar isso "de uma vez só" num PR gigante — tratar por endpoint quando ele for revisado.
5. **Organização de pacotes mista.** A maior parte do código é organizada por camada técnica (`controller/service/repository/model/dto/mapper`); o módulo `ia` é organizado por feature. Ambas são estratégias válidas isoladamente, mas convivem sem uma diretriz explícita documentada até este momento. Não migrar um estilo para o outro sem decisão consciente — o custo de mover ~150 arquivos não se paga sozinho.
6. **`ddl-auto=update` em vez de ferramenta de migration (Flyway/Liquibase).** Já causou um incidente documentado (remoção de coluna `NOT NULL` que ficou órfã no Postgres e quebrou o seeder — ver histórico do projeto). `update` nunca remove/relaxa colunas quando um campo sai da entidade. Continua sendo a estratégia do projeto; ao remover/alterar um campo `@Column`, é preciso draftar o `ALTER TABLE` manual junto (ver `docs/spring-boot-guidelines.md`).
7. **`DevDataResetSeeder` tem ~730 linhas.** É um script de seed sequencial e declarativo (não tem branches/lógica complexa), então o tamanho por si só não é necessariamente um problema de Clean Code — mas é candidato a revisão se crescer mais, ou se passar a ter lógica condicional real.
8. **Pacotes de teste e produção com nomes diferentes.** O código de produção usa o pacote `studojurata_api` (raiz `src/main/java/studojurata_api`), enquanto o único teste existente usa `br.com.studojurata.studojurata_api` (`src/test/java/br/com/studojurata/studojurata_api`). Não é um bug funcional (Maven/Spring não exigem que coincidam), mas é uma inconsistência a alinhar quando novos testes forem adicionados — usar `studojurata_api` como pacote-raiz dos testes, espelhando a produção.
9. **Logs de execução (`backend_run*.log`) e `target/` foram versionados/deixados na raiz do repositório.** `target/` está no `.gitignore`; os `backend_run*.log` (dezenas de arquivos) não estão listados — verificar se foram commitados por engano.

## 7. Onde colocar cada tipo de lógica (regra para novas features)

- **Validação de formato/obrigatoriedade de campo** → Bean Validation no `RequestDTO` (`@NotNull`, `@NotBlank`, etc.), nunca no controller.
- **Validação que depende de estado do banco** (ex.: "só posso lançar se todas as questões estão aprovadas") → Service, lançando `RegraNegocioException`/`RequisicaoInvalidaException`.
- **Resolução de relacionamento a partir de um ID recebido no DTO** → Mapper (`toEntity`), lançando `RecursoNaoEncontradoException` se o ID não existir.
- **Autorização por papel** → `SecurityConfig` (`requestMatchers`).
- **Autorização por dono do recurso** → `AlunoAccessGuard` (ou guard equivalente, se um padrão parecido for necessário para Professor/Responsável no futuro).
- **Efeitos colaterais de negócio** (notificar responsável, registrar auditoria, recalcular nota) → dentro do Service dono da operação, chamando o service do efeito colateral (`NotificacaoService`, `AuditLogService`) — nunca disparado do controller.
- **Chamada a serviço externo** (ex.: Gemini) → cliente dedicado atrás de uma interface pequena (`GeminiQuestaoClient`), com exceção própria (`GeminiIndisponivelException`) e fallback tratado no service que o consome. Esse é o único lugar do projeto com interface de "porta" — justificado porque existe (ou é esperado) mais de uma implementação/necessidade de fallback, não por preferência estilística.

## 8. Dependências entre camadas (regra de sentido)

```
controller → mapper, service
mapper     → repository (só para resolver relações), dto, model
service    → repository, outros services (efeitos colaterais), model
repository → model
```

Nunca o inverso: `repository` não conhece `service`; `model`/`entity` não conhece `dto`, `mapper`, `controller` nem `service`; `service` não constrói `ResponseEntity`/DTO de resposta HTTP diretamente (isso é papel do controller + mapper).
