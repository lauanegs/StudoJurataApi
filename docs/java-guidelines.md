# Java Guidelines — StudoJurataApi

Java 21 (LTS), compilado via Maven (`spring-boot-starter-parent` 3.3.4). Estas regras descrevem o que o projeto **já faz** e onde aplicar critério ao estender.

## Nomenclatura

- Classes: `PascalCase`, substantivo do domínio (`Simulado`, `SimuladoAluno`, `AlunoAccessGuard`). Sufixos técnicos indicam o papel: `...Controller`, `...Service`, `...Repository`, `...Mapper`, `...RequestDTO`/`...ResponseDTO`, `...Exception`.
- Métodos: `camelCase`, verbo + (opcional) complemento, em português, consistente com o resto do código (`buscar`, `salvar`, `resolverElegiveis`, `historicoPorAlunoEDisciplina`). Não misturar inglês (`save`, `find`) no mesmo tipo de camada onde o resto usa português.
- Constantes: `UPPER_SNAKE_CASE` — hoje o projeto tem poucas (a maioria dos "valores fixos" são enums, não `static final` soltos). Prefira enum a constante solta quando o valor representa um conjunto fechado de opções de domínio.
- Pacotes: minúsculo, sem underscore extra além do já usado no nome raiz (`studojurata_api`). **Nota:** o pacote raiz de produção é `studojurata_api`; o único teste existente está em `br.com.studojurata.studojurata_api` (inconsistente — ver `docs/architecture.md` #8). Novos testes devem usar `studojurata_api` como raiz, espelhando produção, em vez de replicar a inconsistência.

## Organização de imports

- Sem wildcard (`import studojurata_api.model.*`) na maior parte do código — **exceção real e aceita**: `DevDataResetSeeder.java` usa `import studojurata_api.model.*;` e `import studojurata_api.model.enums.*;` porque referencia dezenas de classes desses pacotes; nesse caso específico o wildcard reduz ruído em vez de aumentar. Fora desse tipo de classe (seeders/scripts que tocam o modelo inteiro), prefira imports explícitos.
- Sem import estático de métodos, exceto o que já vier de libs (nenhum caso hoje). Evite introduzir sem necessidade clara.

## Classes

- Uma classe pública por arquivo, nome do arquivo = nome da classe (padrão Java, já seguido 100%).
- Entidades JPA: `@Entity`, estendem `BaseEntity`, usam Lombok `@Getter @Setter @EqualsAndHashCode(callSuper = true)`. **Nunca `@Data`** em entidade (gera `equals`/`hashCode`/`toString` por todos os campos, o que quebra com relações `@ManyToOne`/`@OneToMany` e proxies lazy do Hibernate — `equals`/`hashCode` de entidade deve considerar só o `id`, como já está centralizado em `BaseEntity`).
- Services/Controllers/Mappers/Repositories: sem interface própria, a não ser que exista mais de uma implementação real ou uma necessidade concreta de abstração (ver `GeminiQuestaoClient`/`GeminiApiClient`, o único par interface/implementação do projeto, justificado por fallback e possibilidade de outro provedor de IA no futuro).

## Métodos

- Injeção de dependência sempre por construtor via `@RequiredArgsConstructor` sobre campos `private final`. Nunca `@Autowired` em campo.
- Visibilidade mínima necessária: métodos auxiliares de um service que não são chamados de fora (`resolverElegiveis`, `corpo()` no `GlobalExceptionHandler`) são `private`. Não exponha como `public` "para o caso de precisar depois".
- Prefira retorno de tipo concreto (`List<Simulado>`) a tipos genéricos demais (`Collection<?>`) quando o chamador precisa de operações de `List`.

## Atributos

- Sempre `private`, acessados via Lombok `@Getter`/`@Setter` (entidades/DTOs) — nunca campo público.
- Em Service/Controller/Mapper, atributos são sempre `private final` (dependências injetadas), nunca mutáveis após construção.

## Constructors

- Construtor gerado por `@RequiredArgsConstructor` é o padrão do projeto para injeção — não escrever construtores manuais equivalentes.
- Entidades JPA precisam do construtor padrão sem argumentos exigido pelo Hibernate; Lombok não gera isso automaticamente aqui (o projeto não usa `@NoArgsConstructor` explícito porque as entidades não têm outro construtor declarado — o compilador já fornece o construtor padrão implícito). Se um construtor com argumentos for adicionado a uma entidade no futuro, será necessário `@NoArgsConstructor` explícito nesse ponto.

## Records

O projeto **não usa `record`** em nenhum lugar hoje — todos os DTOs são classes Lombok mutáveis (`@Getter @Setter`). Isso é consistente com o fato de que os DTOs precisam ser desserializados pelo Jackson via setters e, em alguns casos, mutados após a criação. **Não convertam DTOs existentes para `record`** só por serem imutáveis por natureza — isso muda o contrato de serialização/desserialização e pode quebrar compatibilidade sem necessidade. `record` é uma opção aceitável para um DTO **novo**, imutável por design e sem necessidade de setters (ex.: um DTO de resposta simples, tipo `ErrorResponse` já é candidato natural — mas convertê-lo agora é uma mudança de contrato a sinalizar antes, não a fazer silenciosamente).

## Enums

- Todo enum de domínio fica em `model/enums/` (ou `ia/model/enums/` para o módulo de IA), mapeado com `@Enumerated(EnumType.STRING)` — nunca `ORDINAL` (ordinal quebra silenciosamente se a ordem dos valores mudar). Manter esse padrão em qualquer enum novo.
- Enums são usados tanto para regra de negócio quanto para autorização (`TipoUsuario` alimenta `hasRole()` do Spring Security) — ao renomear um valor de enum, verificar `SecurityConfig` e qualquer string mágica correspondente no front-end.

## Optional

Ver `docs/clean-code.md` — resumo: use como tipo de retorno de busca que pode falhar, resolva com `.orElseThrow()` no ponto de uso, nunca como campo de entidade/DTO nem como parâmetro de método.

## Collections

- Interface pública sempre `List<T>` (nunca `ArrayList<T>` como tipo de retorno/parâmetro).
- `.toList()` (Java 16+) em vez de `.collect(Collectors.toList())` para o caso comum de lista imutável resultante de stream.
- Sem uso de `Set`/`Map` como retorno de API até o momento — se precisar de deduplicação, prefira resolver na query (`DISTINCT` via query method) a filtrar em memória, quando possível.

## Streams

Ver `docs/clean-code.md`. Regra objetiva: streams de 1 a 3 operações encadeadas (`filter`/`map`/`collect`/`toList`) são preferidas a loop; além disso, ou com lambdas multi-linha complexas, prefira loop `for` explícito.

## Exceptions

- Domínio: `RecursoNaoEncontradoException`, `RegraNegocioException`, `RequisicaoInvalidaException` — reusar, não recriar por feature.
- Nunca capturar `Exception` genérico só para logar e engolir. A única captura ampla aceitável hoje é em `GeminiApiClient.gerarQuestoes()`, que envolve uma chamada de rede externa cujas falhas (timeout, JSON malformado, IOException) todas devem virar `GeminiIndisponivelException` — está documentado no próprio código como decisão consciente (fallback), não como atalho.
- Exceções customizadas devem estender `RuntimeException` (como as três atuais) para não poluir assinaturas de método com `throws` — o projeto não usa checked exceptions próprias.

## Generics

Uso hoje é limitado a `JpaRepository<Entity, Long>` e coleções padrão (`List<T>`). Não introduzir generics próprios (`Repository<T, ID>` customizado, `Result<T, E>`, etc.) sem um caso de uso concreto que o Spring Data já não resolva.

## Nullability

Sem `@Nullable`/`@NonNull` (JSR-305 ou Spring) anotados hoje no código. Não é necessário introduzir essas anotações em massa — mas ao escrever um método novo cujo contrato de null não é óbvio pelo nome, prefira documentar em Javadoc curto (`/** @return null se o usuário não tiver escola associada */`, como já é feito em `EscolaContext`) a adicionar anotações que o resto do projeto não usa.

## Imutabilidade

- Entidades e DTOs são mutáveis por design (Hibernate/Jackson exigem setters). Não é uma falha de Clean Code neste contexto — é o modelo de objeto que o JPA e o Jackson do projeto assumem.
- Onde imutabilidade *é* possível e já é seguida: variáveis locais que não precisam ser reatribuídas devem ser `final` quando isso aumenta clareza (não é 100% consistente no código atual — aplicar ao tocar um método, não em passada de reformatação isolada).

## Visibilidade

Padrão: `public` só na API de fato pública da classe (métodos de controller, métodos de service chamados por controller/outro service, getters/setters de Lombok). Métodos auxiliares internos: `private`. Não existe uso de `protected`/package-private hoje — não introduzir sem motivo (ex.: extensão de classe entre pacotes, que não ocorre no projeto).

## Formatação

- Indentação de 4 espaços, chaves na mesma linha (`public class X {`), como 100% do código já segue.
- Sem formatter automatizado configurado no `pom.xml` hoje (sem plugin `spotless`/`google-java-format`). Ao editar um arquivo, seguir a formatação já presente nele — não reformatar o arquivo inteiro numa mudança que deveria ser pontual.

## Organização de código dentro da classe

Ordem observada e a manter: campos (dependências injetadas) → métodos públicos na ordem de uso mais comum (CRUD: listar, buscar, salvar/criar, atualizar, deletar, depois métodos de negócio específicos) → métodos privados auxiliares por último, próximos de quem os usa.

## Referências
- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) — usado como referência de formatação/organização, não copiado literalmente (o projeto usa 4 espaços, o guia do Google usa 2; manter o padrão já estabelecido no projeto).
- [Effective Java, 3ª ed.](https://www.oreilly.com/library/view/effective-java-3rd/9780134686097/) — itens sobre enums, generics, exceções.
- [Documentação oficial do Java 21 (Oracle/OpenJDK)](https://docs.oracle.com/en/java/javase/21/).
