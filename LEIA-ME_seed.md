# Script de reset + carga de dados (DevDataResetSeeder)

Arquivo: `src/main/java/studojurata_api/config/DevDataResetSeeder.java`

## O que ele faz

1. **Apaga todos os registros de todas as tabelas** do banco configurado em
   `application.properties` (respeitando a ordem de dependência das FKs —
   não precisa rodar `DROP`/`TRUNCATE` manual em SQL).
2. **Recria um conjunto mínimo de dados em todas as entidades**:
   - 1 escola, 1 curso, 2 disciplinas (Matemática, Português)
   - 2 turmas (com horários semanais)
   - 2 professores + 1 administrador (com login/usuário)
   - 4 alunos (2 por turma), cada um com login e matrícula ativa
   - 1 responsável por aluno (com aceite de termos)
   - Plano de ensino, conteúdos, planos de aula e aulas para cada disciplina/turma
   - Frequência (chamada) registrada nas aulas
   - 1 simulado por turma, com **5 questões cada** (3 de múltipla escolha + 2
     de verdadeiro/falso, cada uma com suas alternativas)
   - Tentativas de simulado respondidas pelos alunos (nota, acertos, respostas)
   - Notas por disciplina/período
   - Eventos, log de auditoria
   - Gamificação (skins, pontuação e skin equipada por aluno)
   - Módulo de IA (revisão espaçada e histórico de geração)

## Como rodar (sem risco de rodar sozinho em produção)

O seeder só executa quando o profile **`seed`** está ativo.

- **Pela IDE (IntelliJ/Eclipse/VS Code):** na configuração de execução da
  aplicação, adicione `seed` em "Active profiles" (ou a VM option
  `-Dspring.profiles.active=seed`) e rode normalmente.
- **Pelo terminal com Maven:**
  ```
  mvn spring-boot:run -Dspring-boot.run.profiles=seed
  ```
- **Rodando o `.jar` já empacotado:**
  ```
  java -jar target/studojurata-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=seed
  ```

Sem o profile `seed`, a aplicação sobe normalmente e o seeder **não roda** —
então não há risco de apagar dados sem querer em produção.

## Onde colocar o arquivo

Ele já foi salvo na pasta correta do projeto:
`StudoJurataApi/src/main/java/studojurata_api/config/DevDataResetSeeder.java`
(mesmo pacote do `GamificacaoSeeder.java` já existente, que continua
funcionando normalmente — as 3 skins que ele cria já são criadas pelo novo
seeder, então ele não duplica nada).

## Logins criados

| Usuário       | Senha      | Papel          |
|---------------|------------|----------------|
| admin         | admin123   | ADMINISTRADOR  |
| joao.silva    | senha123   | PROFESSOR      |
| maria.souza   | senha123   | PROFESSOR      |
| aluno.pedro   | senha123   | ALUNO (Turma A)|
| aluno.beatriz | senha123   | ALUNO (Turma A)|
| aluno.lucas   | senha123   | ALUNO (Turma B)|
| aluno.camila  | senha123   | ALUNO (Turma B)|

## Atenção

Esse script **apaga todos os dados existentes** no banco apontado por
`spring.datasource.url` em `application.properties` antes de recriar os
dados de exemplo. Use apenas em ambiente de desenvolvimento/teste, nunca
apontando para um banco de produção.
