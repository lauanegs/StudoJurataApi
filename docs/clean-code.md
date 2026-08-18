# Clean Code — guia prático (StudoJurataApi)

> Nenhuma regra aqui é absoluta. Cada seção explica quando a regra se aplica e quando ignorá-la é a decisão certa. O critério final é sempre: **a versão mais simples que preserva comportamento, legibilidade e testabilidade vence.**

## Nomes significativos

O projeto já segue um padrão consistente: **nomes de domínio em português**, alinhados ao vocabulário do negócio (`Simulado`, `Aluno`, `Matricula`, `lancar()`, `encerrar()`, `historicoPorAluno()`). Métodos de CRUD seguem `listar/buscar/salvar/atualizar/deletar`. **Manter esse vocabulário** — não misturar `save`/`find`/`create` em inglês com o restante em português dentro da mesma classe ou camada.

- Nome de método deve dizer o que ele faz, não como (`recalcular(alunoId, disciplinaId, periodoLetivo)`, não `processar(...)`).
- Booleans como pergunta: `mesmoAluno`, `naoAprovadas.isEmpty()`. Evitar nomes ambíguos como `flag`, `check`, `data` genérico.
- Evitar abreviações que não sejam óbvias no domínio (`qtd` é aceitável e já usado; `tmp`, `obj` genérico para uma entidade específica, não).
- Quando um nome só faz sentido com um comentário do lado, o nome está errado — renomear em vez de comentar.

**Quando abrir exceção:** parâmetros de mapeamento genérico de baixo nível (ex.: `obj` em `SimuladoService.salvar(Simulado obj)`) são aceitáveis em métodos de uma linha onde o tipo já é auto-explicativo; não vale a pena reescrever isso "só por Clean Code" sem tocar no método por outro motivo.

## Métodos pequenos

Prefira métodos que fazem uma coisa e a nomeiam. `SimuladoService.lancar()` é um bom exemplo real do projeto: o método principal orquestra 5 passos claros (buscar, validar status, validar questões, resolver elegíveis, criar `SimuladoAluno`s), e a resolução de elegibilidade já está extraída em `resolverElegiveis()` porque tem uma ramificação própria (TODOS vs ESPECIFICO) e seu próprio tratamento de erro.

**Quando não extrair:** um método de 15-20 linhas que executa passos sequenciais e sem ramificação (ex.: montar um DTO campo a campo em um mapper) é mais legível *inteiro* do que fatiado em `montarCampo1()`, `montarCampo2()`, `montarCampo3()` — fragmentar aqui adiciona indireção sem reduzir complexidade real. Extraia quando um trecho (a) tem uma ramificação/condição própria que merece nome, (b) é reusado em mais de um lugar, ou (c) o método principal ficou difícil de ler de cima a baixo sem rolar a tela.

## Classes coesas / responsabilidade única

Um Service por entidade/agregado é o padrão do projeto e deve ser mantido: `SimuladoService` cuida do ciclo de vida do Simulado, não da nota do aluno (isso é `NotaService`, chamado como colaborador). Quando um Service começa a depender de mais de ~5-6 repositórios/services, é sinal de que ele pode estar assumindo responsabilidades de outro agregado — mas isso é um sinal para investigar, não uma regra automática de quebrar a classe.

**Quando não separar:** não criar uma classe nova para "isolar" 3-4 linhas de lógica que só é usada em um lugar. Isso troca uma responsabilidade clara por indireção sem ganho real.

## Redução de complexidade

- Prefira `return` antecipado (guard clauses) a `if/else` aninhado. Exemplo já usado no projeto: `AlunoAccessGuard.garantir()` lança e retorna cedo em vez de aninhar o caminho feliz.
- Extraia uma condição composta longa para uma variável ou método com nome (`boolean mesmoAluno = ...`) em vez de inline num `if`.
- Streams com `.filter().map().collect()` de até 2-3 operações são preferíveis a loops manuais quando deixam a intenção mais clara (ver `SimuladoService.resolverElegiveis`). Streams com mais de 3-4 operações encadeadas, ou com lógica condicional complexa dentro do lambda, costumam ficar mais difíceis de debugar que um loop `for` explícito — nesse ponto, prefira o loop.

## Eliminação de duplicação (DRY sem abstração artificial)

O projeto tem duplicação **estrutural** real: ~30 services repetem o mesmo esqueleto (`listar/buscar/salvar/atualizar/deletar` delegando a um `JpaRepository`). **Isso não deve virar uma classe genérica `AbstractCrudService<T, ID>`** — cada service, mesmo repetitivo, tem (ou vai ganhar) regras específicas (validações de status, efeitos colaterais, filtros por escola), e uma abstração genérica either força todo mundo a herdar comportamento que não serve, ou vira um `if instanceof` disfarçado. Duplicação de 5 linhas de boilerplate é mais barata de manter do que uma hierarquia genérica errada.

DRY vale quando a duplicação é de **regra de negócio**, não de esqueleto sintático: se a mesma validação de "questão precisa estar aprovada antes de X" aparecer em dois services diferentes, *isso* é candidato a extrair (para um método utilitário ou para o próprio service dono da regra, chamado como colaborador) — porque um dia as duas cópias vão divergir por engano.

## Código morto

Remover imports não usados, métodos privados não chamados, DTOs/classes sem nenhuma referência. Antes de remover algo que parece morto, confirmar com busca (`grep`/IDE "find usages") — o projeto tem comentários referenciando "compatibilidade com código legado" (ex.: `NoSuchElementException` no `GlobalExceptionHandler` existe só por causa de `orElseThrow()` sem argumento em services antigos), então "não usado à primeira vista" pode estar sendo usado por reflexão, serialização Jackson, ou por um caminho de exceção indireto.

## Tratamento de exceções

O projeto já tem três exceções de domínio bem definidas — **usá-las, não criar novas por endpoint**:

- `RecursoNaoEncontradoException` → algo não existe (404).
- `RegraNegocioException` → estado atual não permite a operação (409). Ex.: "simulado já foi lançado".
- `RequisicaoInvalidaException` → o pedido em si é inválido, independente de estado (400). Ex.: "informe ao menos um aluno".

Nunca deixar uma exceção técnica (`NullPointerException`, `SQLException`) vazar para o cliente como 500 quando ela representa um caso de negócio esperado — traduza para uma das três acima no ponto em que o caso é identificado. Se um novo tipo de erro realmente não se encaixa em nenhuma das três, é aceitável adicionar uma quarta — mas registre o `@ExceptionHandler` correspondente no `GlobalExceptionHandler`, nunca trate ad-hoc dentro do controller.

## Validações

- Formato/obrigatoriedade de campo → Bean Validation no DTO (`@NotBlank`, `@NotNull`, `@Positive`, etc.), não `if` manual no service.
- Regra que depende de outros dados (banco, outro campo) → sempre no service, nunca no DTO/controller.
- Mensagens de validação em português, curtas e acionáveis (`"periodoLetivo é obrigatório"`), como já é o padrão em `SimuladoRequestDTO`.

## Comentários

O projeto usa comentários Javadoc/bloco para **explicar o "porquê"**, não o "o quê" — e isso inclui um padrão específico deste código: referenciar o item da "Análise Crítica" que motivou uma decisão (`// Correção 2.1 da Terceira Análise Crítica (IDOR)...`). Esse rastro é valioso porque documenta uma decisão de segurança/negócio que não é óbvia lendo só o código. **Manter esse estilo** ao tocar código antigo; ao adicionar lógica nova sem uma "análise crítica" associada, explicar o porquê da decisão em prosa normal.

Não comentar o óbvio (`// busca o simulado` acima de `buscar(id)`). Comentário que só repete o nome do método/variável é ruído — remova-o ou substitua por um nome melhor.

## Parâmetros

Os services do projeto raramente passam de 3-4 parâmetros (ex.: `recalcular(alunoId, disciplinaId, periodoLetivo)`). Quando um método precisar de mais que isso, prefira agrupar em um DTO/record existente em vez de acrescentar mais um parâmetro solto — mas não crie um DTO novo só para 2 parâmetros relacionados que cabem numa assinatura curta.

## Condicionais

Evite condicionais booleanas negativas duplas (`if (!naoEncontrado)`). Prefira nomear a condição positiva. Para múltiplos estados (como os enums de status do projeto: `StatusSimulado`, `StatusSimuladoAluno`), prefira `switch` ou comparação direta de enum a strings mágicas — o projeto já usa `@Enumerated(EnumType.STRING)` em 100% dos enums de domínio, mantenha esse padrão.

## Null

O projeto usa `null` para "campo opcional não preenchido" em entidades JPA (`PlanoEnsino`, `dataFim`, etc.) — é o padrão natural do JPA/Hibernate e não deve ser substituído por `Optional<T>` como atributo de entidade (Optional não é `Serializable` e não deve ser usado como campo persistido). Sempre checar null explicitamente antes de acessar relação opcional (`simulado.getTurma() != null ? ... : ...`, como já é feito nos mappers).

## Optional

Use `Optional` no **retorno de métodos que podem legitimamente não encontrar nada** — é exatamente o uso que `JpaRepository.findById()` já retorna, e o padrão do projeto é resolvê-lo imediatamente com `.orElseThrow(() -> new RecursoNaoEncontradoException(...))` dentro do service. Não deixe `Optional` vazar do service para o controller/DTO — resolva-o (ou lance a exceção) antes.

## Streams

Streams são idiomáticas aqui para transformação de coleções simples (`.stream().map(mapper::toResponseDTO).toList()` em praticamente todo controller `listar()`). Evite:
- Streams com efeitos colaterais dentro do `.map()`/`.forEach()` que também fazem I/O (chamada a repositório) misturado com lógica condicional — nesse caso, um `for` é mais fácil de debugar e de logar.
- `.collect(Collectors.toList())` quando `.toList()` (Java 16+) resolve — o projeto já roda Java 21, prefira a forma curta.

## Legibilidade

Prefira o código que se lê de cima para baixo como uma narrativa (ver `SimuladoService.lancar()`): buscar → validar → validar → resolver → agir. Se for necessário pular entre 4 arquivos para entender um fluxo de 10 linhas, é sinal de indireção em excesso — nem sempre errado (às vezes a responsabilidade genuinamente pertence a outra camada), mas vale questionar.

## Testabilidade

Como a cobertura de testes automatizados hoje é praticamente zero (ver `docs/architecture.md`, problema #3), o critério prático de "testável" aqui é: **o service não deve depender de estado estático, `new HttpClient()` direto no construtor, ou `LocalDateTime.now()` espalhado sem necessidade** — isso já é seguido (ver `GeminiApiClient`, que adia a criação do `HttpClient` e recebe tudo via `@Value`/construtor). Ao escrever código novo, mantenha os services fáceis de testar com `@ExtendWith(MockitoExtension.class)` mesmo que o teste ainda não exista — é isso que a injeção por construtor já garante.

## Referências
- Robert C. Martin, *Clean Code* (síntese dos princípios de nomes, funções e comentários).
- [Effective Java, 3ª ed.](https://www.oreilly.com/library/view/effective-java-3rd/9780134686097/) (itens sobre `Optional`, imutabilidade, exceções).
