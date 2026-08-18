# Checklist de Code Review — StudoJurataApi

> Uso pretendido: checklist que a própria Claude aplica ao revisar código deste projeto (mudanças novas ou trechos existentes sob revisão). Cada item remete às regras detalhadas em `clean-code.md`, `java-guidelines.md`, `spring-boot-guidelines.md` e `architecture.md`. Marcar um item como problema exige apontar o arquivo/linha e o impacto concreto — não reportar achados genéricos sem localização.

## Complexidade
- [ ] Algum método tem mais de ~3 níveis de aninhamento (`if` dentro de `if` dentro de `for`)? Poderia usar guard clause?
- [ ] Alguma condição booleana está difícil de ler (negativas duplas, `&&`/`||` misturados sem parênteses)?
- [ ] Algum método faz claramente mais de uma coisa (nome genérico tipo `processar`, `handle`, `executar` sem sufixo específico é um sinal)?

## Duplicação
- [ ] A mesma **regra de negócio** (não só a mesma estrutura de CRUD) está copiada em mais de um service? (Ver `clean-code.md` — duplicação de esqueleto CRUD entre services é aceita; duplicação de regra não é.)
- [ ] Alguma validação (Bean Validation ou regra manual) está reimplementada em vez de reusar `RecursoNaoEncontradoException`/`RegraNegocioException`/`RequisicaoInvalidaException`?

## Responsabilidades
- [ ] O controller tem alguma lógica de negócio (`if`, cálculo, decisão) em vez de só delegar a mapper/service?
- [ ] O service está fazendo algo que é responsabilidade de outra camada (montar `ResponseEntity`, checar `HttpStatus`, mexer em `HttpServletRequest`)?
- [ ] O mapper está fazendo regra de negócio (além de resolver relação por ID e traduzir campos)?
- [ ] Alguma classe nova foi criada para uma responsabilidade que já cabia em uma existente? Ou o inverso: uma classe existente ganhou uma responsabilidade que não é dela?

## Nomenclatura
- [ ] Nomes de método seguem o vocabulário do projeto (`listar/buscar/salvar/atualizar/deletar` + verbos de domínio em português)?
- [ ] Algum nome é genérico demais (`data`, `obj`, `helper`, `manager`, `processar`) fora do contexto de mapeamento trivial de uma linha?
- [ ] Nome de classe/pacote é consistente com o padrão já existente (sufixo `Controller`/`Service`/`Repository`/`Mapper`/`RequestDTO`/`ResponseDTO`/`Exception`)?

## Arquitetura
- [ ] A mudança respeita o sentido de dependência (`controller → mapper/service → repository → model`, nunca o inverso)? Ver `architecture.md` seção 8.
- [ ] Uma entidade nova estende `BaseEntity` e usa `@Getter @Setter @EqualsAndHashCode(callSuper = true)` (nunca `@Data`)?
- [ ] Um DTO/mapper novo segue o padrão Request/Response + mapper manual, sem introduzir uma lib de mapeamento nova?
- [ ] Foi introduzida uma interface de service/repository sem mais de uma implementação real ou necessidade concreta de abstração?
- [ ] Foi introduzida uma camada nova (Use Case, "Facade", "Manager") que duplica o que Controller→Service já resolve?

## Segurança
- [ ] Toda rota de escrita nova (`POST`/`PUT`/`DELETE`) tem uma entrada explícita em `SecurityConfig` com o papel correto — não depende só do `anyRequest().authenticated()` do final?
- [ ] Um endpoint que recebe `alunoId` (ou equivalente) livre na URL/body verifica posse via `AlunoAccessGuard` (ou guard equivalente) quando o dado é pessoal, e não só o papel do usuário?
- [ ] Um campo sensível novo (senha, token) usa `@JsonProperty(access = WRITE_ONLY)`, nunca `@JsonIgnore` nem serialização direta?
- [ ] Alguma query/endpoint novo devolve dados de outra Escola sem passar por `EscolaContext` quando deveria (ver `architecture.md` #1 — lacuna conhecida, mas não piorar em código novo)?
- [ ] Nenhuma senha/segredo novo foi hardcoded em código ou log (`System.out.println`/`log.info` de senha, token, etc.)?

## Performance
- [ ] Algum `@ManyToOne`/`@OneToMany` novo pode gerar N+1 perceptível num loop (`for` chamando `getX().getY()` que dispara uma query por iteração)?
- [ ] Um endpoint de listagem novo pode devolver uma tabela que cresce sem limite, sem paginação, sem que isso tenha sido avaliado conscientemente (ver `architecture.md` #2)?
- [ ] Alguma stream/coleção está sendo percorrida múltiplas vezes onde uma passada bastaria?

## Tratamento de erros
- [ ] Alguma exceção técnica (`NullPointerException`, `SQLException`, `IOException` não tratada) pode vazar como 500 num caminho que deveria ser um erro de negócio claro (404/400/409)?
- [ ] Uma exceção nova de domínio foi registrada no `GlobalExceptionHandler` com o status HTTP correto?
- [ ] Algum `catch (Exception e)` genérico esconde um erro que deveria propagar (fora do caso já justificado do `GeminiApiClient`)?

## Validação
- [ ] Um DTO de entrada novo tem Bean Validation (`@NotNull`/`@NotBlank`/etc.) nos campos obrigatórios, com mensagem em português?
- [ ] Uma regra que depende de estado do banco está no service (não no DTO/controller)?

## Banco de dados
- [ ] Um campo `@Column` foi removido ou teve `nullable` alterado de `true` para `false` numa entidade já populada, sem o `ALTER TABLE` manual correspondente (lembrar que `ddl-auto=update` não remove/relaxa colunas)?
- [ ] Uma constraint de banco (`unique`, `nullable = false`, FK) foi removida sem entender por que existia?
- [ ] Uma migration manual (se aplicável) foi comunicada/documentada, já que o projeto não usa Flyway/Liquibase?

## JPA
- [ ] `equals`/`hashCode` de uma entidade nova segue o padrão `BaseEntity` (só por `id`)?
- [ ] Enum novo em entidade usa `@Enumerated(EnumType.STRING)`?
- [ ] Relação N:N nova usa entidade de associação explícita (como `AlunoTurma`) em vez de `@ManyToMany` puro, se carregar estado próprio?

## Testes
- [ ] A mudança alterou comportamento de um endpoint sem nenhum teste automatizado cobrindo o caso (situação comum hoje — ver `architecture.md` #3)? Se sim, a verificação manual (chamada real à API, script, Swagger) foi documentada na tarefa/PR?
- [ ] Se um teste foi adicionado, ele usa o pacote `studojurata_api` (não `br.com.studojurata.studojurata_api`, que é a inconsistência legada — ver `architecture.md` #8)?

## Código morto
- [ ] Import, método privado, classe ou DTO sem nenhuma referência no projeto (confirmado via busca, não só "parece não usado")?
- [ ] Código comentado (bloco inteiro de código, não explicação) deixado no arquivo?

## Imports
- [ ] Sem wildcard fora do caso já aceito (seeders que tocam o `model` inteiro)?
- [ ] Sem import não utilizado?

## Dependências
- [ ] Uma dependência nova foi adicionada ao `pom.xml` quando o `java.net.http.HttpClient` nativo (ou outra lib já presente) já resolveria (ver `GeminiApiClient`, que evita uma lib HTTP adicional de propósito)?
- [ ] Se uma lib nova é genuinamente necessária, a razão está clara (não é só "é mais moderno"/"eu prefiro")?

## Possíveis bugs
- [ ] Comparação de enum/String usando `==` onde deveria ser `.equals()` (ou vice-versa, quando `==` é o correto para enum)?
- [ ] Uso de `Optional` que pode lançar `NoSuchElementException` sem tratamento (`.get()` direto em vez de `.orElseThrow()`)?
- [ ] Coleção `null` em vez de vazia sendo devolvida por um método que o chamador itera diretamente?
- [ ] Um `@Transactional` faltando num método que faz múltiplas escritas relacionadas (ou sobrando num método que só lê)?

## Possíveis regressões
- [ ] A mudança altera o formato de uma resposta de API já consumida pelo front (`studojuratafront`)? Campo removido/renomeado/tipo trocado?
- [ ] A mudança altera o comportamento de uma regra de negócio existente sem que isso tenha sido pedido explicitamente?
- [ ] A mudança altera uma constraint de segurança existente (`SecurityConfig`, `AlunoAccessGuard`) de forma que amplia acesso sem essa ser a intenção?
- [ ] Compilação (`mvn compile` ou `mvn -q compile`) e, se aplicável, `mvn test` foram executados após a mudança?

## Referências
Ver seção "Referências" em `clean-code.md`, `java-guidelines.md` e `spring-boot-guidelines.md`.
