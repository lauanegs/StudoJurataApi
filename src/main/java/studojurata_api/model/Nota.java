package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Correção 1.2 + 2.13 da Segunda Análise Crítica ("Nota histórica —
 * decisão confirmada"): Nota deixa de ser um valor único e mutável por
 * (aluno, disciplina) e passa a ser uma série histórica.
 *
 * Correção "matrícula cíclica" (revisão pedagógica): o disambiguador da
 * série histórica deixa de ser periodoLetivo (calendário fixo, incompatível
 * com aluno que entra em qualquer mês do ano) e passa a ser a própria
 * `turma` — cada turma já carrega seu próprio ciclo/matrícula
 * (AlunoTurma.dataInicio), então (aluno, disciplina, turma) preserva o
 * histórico de repetência exatamente como periodoLetivo preservava, sem
 * depender de calendário. Ver NotaService.recalcular.
 *
 * total não é mais um campo livre editável via API: é sempre recalculado
 * por NotaService.recalcular a partir da SOMA de SimuladoAluno.nota dos
 * simulados concluídos daquela disciplina/turma — cada disciplina distribui
 * 100 pontos entre os simulados com notaMaxima > 0 (simulados com
 * notaMaxima = 0 são só reforço/repetição espaçada e não entram na soma) —
 * e só considera simulados aplicados a partir da data de matrícula do aluno
 * naquela turma, para não penalizar quem entrou depois.
 *
 * IMPORTANTE (ddl-auto=update): esta mudança troca a coluna `periodo_letivo`
 * (NOT NULL) por `turma_id`. Como o schema não usa Flyway/Liquibase, `update`
 * não remove a coluna antiga nem preenche a nova para as linhas já
 * existentes, e não consegue criar `turma_id` como NOT NULL numa tabela já
 * populada — por isso `turma` nasce `optional = true` aqui (nullable no
 * banco) e é exigido em NotaService/NotaController via validação de
 * aplicação, não via constraint de coluna (mesmo padrão de
 * `docs/spring-boot-guidelines.md`). Rodar manualmente antes de subir esta
 * versão:
 *   ALTER TABLE nota DROP CONSTRAINT IF EXISTS uk_nota_aluno_disciplina_periodo; -- nome pode variar, conferir com \d nota
 *   ALTER TABLE nota ADD COLUMN turma_id BIGINT REFERENCES turma(id);
 *   -- Nota antiga sem turma correspondente: apagar ou recalcular manualmente
 *   -- via POST /notas/recalcular?alunoId=..&disciplinaId=..&turmaId=.. depois
 *   -- de identificar a turma correta de cada aluno/disciplina.
 *   ALTER TABLE nota DROP COLUMN periodo_letivo;
 *   ALTER TABLE nota ADD CONSTRAINT uk_nota_aluno_disciplina_turma UNIQUE (aluno_id, disciplina_id, turma_id);
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"aluno_id", "disciplina_id", "turma_id"}))
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Nota extends BaseEntity {

    @ManyToOne(optional = false)
    private Aluno aluno;

    @ManyToOne(optional = false)
    private Disciplina disciplina;

    /**
     * Turma em que o aluno cursou a disciplina — disambigua a série
     * histórica (repetência/nova matrícula). `optional = true` só por causa
     * de `ddl-auto=update` numa tabela já populada (ver comentário da
     * classe); na prática é sempre exigido por NotaService.recalcular.
     */
    @ManyToOne(optional = true)
    private Turma turma;

    /**
     * Sempre derivado (ver NotaService.recalcular) a partir da SOMA de
     * SimuladoAluno.nota dos simulados concluídos dessa disciplina/turma —
     * nunca deveria ser setado diretamente pelo cliente da API. Escala
     * 0-100 (soma das notaMaxima dos simulados da disciplina).
     */
    private Double total;

    /** Quantidade de simulados concluídos considerados no cálculo de total (transparência do cálculo). */
    private Integer quantidadeSimuladosConsiderados;
}
