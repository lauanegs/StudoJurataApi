# Processo de Refatoração — StudoJurataApi

> Aplica-se a qualquer refatoração futura conduzida pela Claude neste projeto. O objetivo de toda refatoração aqui é **preservar comportamento** enquanto melhora legibilidade/manutenibilidade — nunca o inverso, e nunca "aproveitar" para redesenhar algo que não foi pedido.

## Regra fundamental antes de tudo

**Não introduzir complexidade para resolver problemas que não existem.** Isso inclui, especificamente para este projeto:
- Não criar interface para um Service/Repository que só tem (e só vai ter) uma implementação.
- Não criar uma camada de Use Case entre Controller e Service.
- Não migrar mappers manuais para MapStruct "de passagem".
- Não introduzir `record` em DTOs existentes sem que isso seja o pedido explícito.
- Não fragmentar um método legível de 15-20 linhas sem ramificação em vários métodos de 3 linhas.
- Não generalizar o esqueleto CRUD dos ~30 services numa classe base genérica.
- Não adicionar paginação, cache, profiles de ambiente, ou qualquer configuração nova "por precaução", sem que o problema concreto que ela resolve esteja presente na tarefa.

Se, ao investigar, uma dessas tentações parecer genuinamente justificada (ex.: uma segunda implementação real de algo apareceu), tratar como uma decisão a **propor e confirmar com o usuário antes de aplicar**, nunca como parte silenciosa de uma refatoração "de Clean Code".

## Ordem do processo

### 1. Entender o código
Ler a classe/método inteiro (não só o trecho aparentemente problemático) e os arquivos diretamente relacionados (o service de um controller, a entidade de um mapper, os testes se existirem). Ler os comentários — neste projeto eles frequentemente documentam *por que* uma decisão de segurança/negócio foi tomada (referências a "Análise Crítica"), e ignorá-los é o erro mais caro possível aqui.

### 2. Identificar o comportamento atual
Antes de mudar qualquer linha, escrever (mentalmente ou em nota) o que o código faz hoje, incluindo casos de borda: o que acontece com `null`, com lista vazia, com um status inesperado, com um usuário sem permissão. Se houver dúvida sobre o comportamento real (não o que o código "parece" fazer), testar manualmente contra a API antes de mexer — não presumir.

### 3. Identificar problemas
Usar o checklist de `docs/code-review.md` como guia. Distinguir:
- **Problema real**: causa um bug, uma regressão potencial, uma violação de segurança, ou torna o código genuinamente difícil de entender/manter.
- **Preferência estilística sem ganho concreto**: não é um problema a corrigir — é ruído.

### 4. Classificar os problemas por gravidade
- **Crítico**: bug ativo, falha de segurança (IDOR, dado sensível exposto, autorização faltando), risco de perda de dados.
- **Alto**: viola uma invariante de negócio documentada (ex.: mexe em `ddl-auto`/schema sem `ALTER TABLE` correspondente), duplicação de regra de negócio (não de esqueleto).
- **Médio**: legibilidade ruim, nomenclatura inconsistente, método fazendo mais de uma coisa.
- **Baixo**: formatação, comentário redundante, import não usado.

Priorizar Crítico e Alto. Não misturar uma correção Crítica com uma limpeza Baixa no mesmo conjunto de mudanças sem necessidade — dificulta revisão e rollback.

### 5. Planejar a alteração
Definir o menor conjunto de mudanças que resolve o problema classificado, respeitando `docs/architecture.md` (onde cada tipo de lógica deve morar) e o padrão já existente na camada tocada (`docs/spring-boot-guidelines.md`/`docs/java-guidelines.md`). Se a alteração:
- muda o formato de uma resposta de API já consumida pelo front,
- muda uma regra de negócio existente,
- muda o schema do banco,
- remove uma funcionalidade,

→ **sinalizar isso ao usuário antes de aplicar**, mesmo que a mudança pareça claramente uma melhoria. Não é uma etapa opcional.

### 6. Fazer a menor alteração necessária
Aplicar só o que foi planejado no passo 5. Resistir a "já que estou aqui" — se outro problema for notado no caminho, anotar separadamente (ex.: via `spawn_task` se disponível) em vez de misturar na mesma mudança.

### 7. Verificar compilação
Rodar `mvn -q compile` (ou `mvnw compile`) após qualquer mudança em `src/main`. Uma refatoração que não compila não está pronta, independente de quão pequena pareça.

```bash
./mvnw compile
```

> Nota do projeto: subir o backend inteiro (`mvnw spring-boot:run`) de dentro de sandboxes de ferramenta pode falhar por um erro de baixo nível de loopback socket do JVM/Windows, sem relação com o código — não é sinal de bug introduzido pela refatoração. `mvn compile`/`mvn test` não têm esse problema (não abrem socket de rede). Se precisar rodar a aplicação de fato, pode ser necessário pedir para o usuário subir manualmente.

### 8. Executar testes
Rodar `mvn test`. Hoje isso cobre essencialmente só `contextLoads()` — não é uma rede de segurança real. Quando a mudança tocar um endpoint/regra de negócio sem teste correspondente, compensar com verificação manual (chamada HTTP real contra a API rodando, Swagger UI, ou um script como os já existentes no front) e **relatar o que foi verificado manualmente**, não só "os testes passaram".

```bash
./mvnw test
```

### 9. Verificar possíveis regressões
Reler o diff inteiro contra a lista do passo 2 (comportamento atual identificado): cada caso de borda que existia antes ainda é tratado? Um `null`/lista vazia que antes não quebrava ainda não quebra? Uma regra de autorização que existia ainda existe? Usar a seção "Possíveis regressões" de `docs/code-review.md`.

### 10. Revisar novamente o código alterado
Ler o diff final como se fosse revisão de outra pessoa (aplicar o checklist completo de `docs/code-review.md`), não só a parte que motivou a mudança.

### 11. Simplificar novamente se ficou complexo demais
Se, ao final, a solução tem mais indireção, mais classes, ou mais linhas do que o problema original justificava, é sinal de que a "correção" foi longe demais. Voltar e simplificar antes de considerar a tarefa concluída — isso vale mesmo que tecnicamente funcione.

## Comunicação de mudanças que afetam comportamento

Ao identificar, em qualquer etapa, que uma alteração necessária muda:
- um contrato de API (formato de request/response),
- uma regra de negócio,
- o schema do banco,
- uma funcionalidade existente,

→ parar, explicar a mudança e o motivo, e esperar confirmação antes de aplicar. Isso vale mesmo dentro de uma tarefa de refatoração "puramente técnica" — Clean Code nunca é motivo suficiente, sozinho, para mudar comportamento observável.
