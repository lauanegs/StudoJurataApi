package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Série histórica por (aluno, disciplina, turma): a turma, e não um período
 * letivo fixo, delimita o ciclo, já que o aluno pode entrar em qualquer mês.
 *
 * total é sempre derivado por NotaService.recalcular: soma das notas dos
 * simulados concluídos da disciplina/turma (cada disciplina distribui 100
 * pontos; simulados com notaMaxima = 0 são só reforço e não entram),
 * considerando só simulados aplicados a partir da matrícula do aluno.
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
     * optional = true só porque ddl-auto=update não cria coluna NOT NULL em
     * tabela populada; NotaService.recalcular sempre exige a turma.
     */
    @ManyToOne(optional = true)
    private Turma turma;

    /** Escala 0-100. Nunca setado pelo cliente da API. */
    private Double total;

    /** Quantidade de simulados concluídos considerados no cálculo de total (transparência do cálculo). */
    private Integer quantidadeSimuladosConsiderados;
}
