# CLAUDE.md — StudoJurataApi

Este é o documento principal de instruções para trabalhar neste repositório. Ele reflete a arquitetura e os padrões **reais** do código, levantados por inspeção direta (não uma arquitetura idealizada). Documentos detalhados complementares em `docs/`:

- [`docs/architecture.md`](docs/architecture.md) — arquitetura real, camadas, fluxo de requisição, problemas identificados.
- [`docs/clean-code.md`](docs/clean-code.md) — guia prático de Clean Code adaptado ao projeto.
- [`docs/java-guidelines.md`](docs/java-guidelines.md) — regras de Java.
- [`docs/spring-boot-guidelines.md`](docs/spring-boot-guidelines.md) — regras de Spring Boot.
- [`docs/code-review.md`](docs/code-review.md) — checklist de revisão.
- [`docs/refactoring-guidelines.md`](docs/refactoring-guidelines.md) — processo de refatoração passo a passo.

## Contexto do projeto

**O que é:** API REST do portal escolar Studo Jurata (backend). Consumida por um front separado em React (`studojuratafront`, repositório irmão).

**Tecnologias e versões:**
- Java 21
- Spring Boot 3.3.4 (`spring-boot-starter-parent`)
- Maven (wrapper `mvnw`/`mvnw.cmd` incluído)
- Spring Data JPA / Hibernate — persistência
- Spring Security — autenticação por **sessão** (cookie `JSESSIONID`), não JWT
- Spring Validation (Bean Validation / Jakarta Validation)
- PostgreSQL (driver `org.postgresql`)
- Lombok — `@Getter`/`@Setter`/`@EqualsAndHashCode`/`@RequiredArgsConstructor` (nunca `@Data` em entidade)
- springdoc-openapi 2.6.0 — Swagger UI (`/swagger-ui/**`, liberado sem auth em dev)
- Integração externa: Google Gemini (`GeminiApiClient`, `java.net.http.HttpClient` nativo, sem lib HTTP adicional)

**Arquitetura:** camadas clássicas (Controller → Mapper → Service → Repository → Entity), organizadas majoritariamente **por tipo técnico**, com uma exceção deliberada: o módulo `ia/` é organizado por feature. Ver [`docs/architecture.md`](docs/architecture.md) para o detalhamento completo, incluindo os "Problemas arquiteturais identificados" — não corrigir esses problemas silenciosamente; eles existem para orientar decisões futuras, algumas exigem confirmação do usuário antes de agir.

**Estrutura principal** (`src/main/java/studojurata_api/`):
```
controller/   — ~31 REST controllers, um por entidade/recurso principal
service/      — ~30 services, regra de negócio, sem interface própria
repository/   — interfaces JpaRepository, query methods derivados
model/        — entidades JPA (estendem BaseEntity) + model/enums/
dto/          — RequestDTO/ResponseDTO por entidade
mapper/       — conversores DTO ↔ Entity, manuais (sem MapStruct)
exception/    — exceções de domínio + GlobalExceptionHandler
security/     — CustomUserDetails(Service), AlunoAccessGuard, EscolaContext
config/       — SecurityConfig, seeders de dev
ia/           — módulo de IA (Gemini), organizado por feature própria
```

**Banco de dados:** PostgreSQL local (`jdbc:postgresql://localhost:5432/studojurata`), schema gerenciado por `spring.jpa.hibernate.ddl-auto=update` — **sem** Flyway/Liquibase. `update` nunca remove/relaxa colunas; ao remover ou restringir um campo de entidade já populada, é necessário draftar o `ALTER TABLE` manual junto (já causou um incidente real documentado em `docs/architecture.md`).

**Build:** Maven (`./mvnw compile`, `./mvnw test`, `./mvnw spring-boot:run`). Subir a aplicação completa de dentro de um sandbox de ferramenta pode falhar por um erro de baixo nível de loopback socket do JVM/Windows (não relacionado ao código) — se acontecer após 2-3 tentativas, pedir para o usuário subir manualmente em vez de insistir com variações de flags de JVM.

**Testes:** JUnit 5 + `spring-boot-starter-test` + `spring-security-test` no `pom.xml`, mas cobertura real hoje é **essencialmente zero** (só existe `StudojurataApiApplicationTests.contextLoads()`). Ao alterar comportamento sem teste correspondente, compensar com verificação manual documentada (chamada real à API, Swagger, script) — ver `docs/refactoring-guidelines.md`.

## Regra fundamental: simplicidade acima de tudo

**A simplicidade deve ser priorizada.** Não criar abstrações, interfaces, classes, métodos, padrões de projeto ou camadas apenas para "seguir Clean Code". Uma solução mais simples deve ser preferida sempre que oferecer o mesmo comportamento, legibilidade e manutenibilidade. Não transformar código simples em arquitetura excessivamente complexa.

**Não introduzir complexidade para resolver problemas que não existem.** Especificamente neste projeto, evitar:
- abstrações prematuras (interface para Service/Repository com uma única implementação real);
- design patterns desnecessários;
- classes com uma única função sem justificativa concreta;
- métodos fragmentados além do que a legibilidade exige;
- wrappers desnecessários em torno de APIs do Spring/JPA que já são simples;
- camadas extras (Use Case entre Controller e Service, "Facade", "Manager");
- configuração/generalização prematura (paginação, cache, profiles — só quando o problema concreto existir).

O único par interface/implementação do projeto (`GeminiQuestaoClient`/`GeminiApiClient`) existe porque há uma razão real (fallback documentado, possibilidade de outro provedor) — é o padrão de quando uma abstração *é* justificada, não a exceção a ser generalizada.

## Regras de desenvolvimento

### Clean Code / SOLID / KISS / DRY
Ver [`docs/clean-code.md`](docs/clean-code.md) para o guia completo com exemplos do próprio código. Resumo:
- **SRP** já é seguido por service/agregado (um `SimuladoService`, não um `SimuladoENotaService`) — manter.
- **DRY** aplica-se a duplicação de *regra de negócio*, não ao esqueleto CRUD repetido nos ~30 services (isso é aceito; não generalizar numa classe base).
- **KISS**: ver seção acima.
- **Alta coesão / baixo acoplamento**: cada camada só conhece a camada imediatamente abaixo (ver `docs/architecture.md` seção 8, regra de sentido de dependência).

### Nomenclatura
Vocabulário de domínio em português, consistente com o já existente: `listar/buscar/salvar/atualizar/deletar` para CRUD, verbos de negócio explícitos para ações (`lancar`, `encerrar`, `recalcular`). Não misturar inglês numa camada que já usa português. Ver [`docs/java-guidelines.md`](docs/java-guidelines.md).

### Tratamento de erros
Usar sempre as três exceções de domínio existentes (`RecursoNaoEncontradoException` → 404, `RegraNegocioException` → 409, `RequisicaoInvalidaException` → 400), tratadas centralmente em `GlobalExceptionHandler`. Nunca deixar uma exceção técnica vazar como 500 quando representa um caso de negócio esperado.

### Validações
Forma/obrigatoriedade → Bean Validation no DTO. Regra dependente de estado do banco → service. Cobertura de validação hoje é inconsistente (6 de 17 DTOs) — não é preciso uniformizar tudo de uma vez, mas ao tocar um DTO sem validação, considerar adicionar (sinalizando se isso muda o comportamento de erro de um endpoint já em uso).

### Segurança
- Sessão + cookie (não JWT). BCrypt sempre para senha. Campo sensível: `@JsonProperty(access = WRITE_ONLY)`, nunca `@JsonIgnore`.
- Autorização por papel: `SecurityConfig` (matchers explícitos por rota + método HTTP). Toda rota de escrita nova precisa de uma entrada própria.
- Autorização por dono do recurso (IDOR): `AlunoAccessGuard` para dados pessoais de Aluno — papel sozinho não basta.
- Multi-tenant (`EscolaContext`) é parcial hoje (5 de 30 services) — não presumir que é o padrão universal nem "consertar" isso silenciosamente; é uma decisão de produto a confirmar.
- Nunca commitar segredo real; a senha do Postgres em `application.properties` hoje é texto puro aceitável só porque é 100% ambiente local de dev — sinalizar antes de qualquer preparação para produção.

### Testes
Cobertura real é quase zero. Ao alterar comportamento, verificar manualmente contra a API e relatar o que foi verificado. Se testes forem adicionados, usar o pacote `studojurata_api` (espelhando produção — não `br.com.studojurata.studojurata_api`, que é a inconsistência legada do único teste hoje).

### Manutenção
Seguir `docs/refactoring-guidelines.md` para qualquer refatoração: entender → identificar comportamento e problemas → classificar por gravidade → planejar a menor mudança → aplicar → compilar → testar → verificar regressão → revisar de novo → simplificar se necessário.

## Preservação de comportamento

Ao refatorar ou revisar código existente:
- **Preservar o comportamento funcional existente.**
- **Não alterar regras de negócio sem necessidade.**
- **Não alterar contratos da API sem justificativa** (o front `studojuratafront` consome esta API diretamente — um campo renomeado/removido quebra o front sem aviso).
- **Não alterar banco de dados sem necessidade** (e nunca sem o `ALTER TABLE` manual correspondente quando `ddl-auto=update` não resolveria sozinho).
- **Não remover funcionalidades existentes.**
- **Não substituir uma implementação funcional apenas por preferência pessoal** (ex.: trocar mapper manual por MapStruct, DTO mutável por `record`, sem que isso resolva um problema real).

**Quando uma alteração puder mudar comportamento observável, sinalizar antes de realizá-la e esperar confirmação** — isso não é opcional, mesmo dentro de uma tarefa de "só Clean Code".

## Referências gerais
Ver a seção "Referências" ao final de cada documento em `docs/` para as fontes oficiais usadas como base (Spring Boot/Spring Data/Spring Security Reference, Effective Java, Google Java Style Guide, Clean Code de Robert C. Martin).
