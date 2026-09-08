# Script de reset + carga de dados (DevDataResetSeeder)

Arquivo: `src/main/java/studojurata_api/config/DevDataResetSeeder.java`

## O que ele faz

1. **Apaga todos os registros de todas as tabelas** do banco configurado em
   `application.properties` (respeitando a ordem de dependência das FKs —
   não precisa rodar `DROP`/`TRUNCATE` manual em SQL).
2. **Recria os dados reais da escola administrada** (cursos de tecnologia
   infantojuvenil):
   - 1 escola, 4 cursos (Geek Júnior, Robótica, Programação Gamificada,
     Geek Teens), 2 disciplinas (Robótica, Programação Gamificada) — Robótica
     e Programação Gamificada só têm a matéria correspondente; Geek Júnior e
     Geek Teens têm as duas
   - 4 turmas (1 por curso, 1 aula semanal de 1h30, capacidade 8 alunos cada)
   - 2 professores titulares (1 por disciplina) + 1 administrador (com login/usuário)
   - 16 alunos de 7 a 14 anos: 14 com matrícula **ATIVA**, 1 com matrícula
     **CONCLUIDA** e 1 **CANCELADA** — cobrindo as situações de matrícula
   - 1 responsável por aluno (com aceite de termos)
   - Plano de ensino, conteúdos, planos de aula e aulas para cada disciplina/turma
   - Frequência (chamada) registrada nas aulas
   - 1 simulado por turma/disciplina (6 no total), com **5 questões cada**
     (3 de múltipla escolha + 2 de verdadeiro/falso) — parte das tentativas já
     respondida (CONCLUIDO), parte ainda **PENDENTE** ("a fazer" na tela do aluno)
   - 4 questões de origem IA com status **PENDENTE**, reunidas em 2 simulados
     RASCUNHO ("Revisão IA") — aparecem na tela de revisão do professor
   - Notas por disciplina/turma
   - Eventos de aula demonstrativa (1 por curso) + reunião de pais, log de auditoria
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

| Usuário          | Senha    | Papel         | Turma / Curso                     |
|------------------|----------|---------------|------------------------------------|
| admin            | admin123 | ADMINISTRADOR | —                                   |
| rafael.mendes    | senha123 | PROFESSOR     | Titular de Robótica                 |
| juliana.costa    | senha123 | PROFESSOR     | Titular de Programação Gamificada   |
| aluno.enzo       | senha123 | ALUNO         | Geek Júnior (ativa)                 |
| aluno.alice      | senha123 | ALUNO         | Geek Júnior (ativa)                 |
| aluno.davi       | senha123 | ALUNO         | Geek Júnior (ativa)                 |
| aluno.sophia     | senha123 | ALUNO         | Geek Júnior (ativa)                 |
| aluno.miguel     | senha123 | ALUNO         | Robótica (ativa)                    |
| aluno.laura      | senha123 | ALUNO         | Robótica (ativa)                    |
| aluno.gabriel    | senha123 | ALUNO         | Robótica (ativa)                    |
| aluno.isabela    | senha123 | ALUNO         | Robótica (ativa)                    |
| aluno.bernardo   | senha123 | ALUNO         | Programação Gamificada (ativa)      |
| aluno.manuela    | senha123 | ALUNO         | Programação Gamificada (ativa)      |
| aluno.heitor     | senha123 | ALUNO         | Programação Gamificada (ativa)      |
| aluno.yasmin     | senha123 | ALUNO         | Geek Teens (ativa)                  |
| aluno.arthur     | senha123 | ALUNO         | Geek Teens (ativa)                  |
| aluno.luiza      | senha123 | ALUNO         | Geek Teens (ativa)                  |
| aluno.theo       | senha123 | ALUNO         | Robótica (matrícula **concluída**)  |
| aluno.valentina  | senha123 | ALUNO         | Geek Teens (matrícula **cancelada**)|

## Atenção

Esse script **apaga todos os dados existentes** no banco apontado por
`spring.datasource.url` em `application.properties` antes de recriar os
dados de exemplo. Use apenas em ambiente de desenvolvimento/teste, nunca
apontando para um banco de produção.
