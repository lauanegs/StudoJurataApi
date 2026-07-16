package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Correção 1.2 + 2.13 da Segunda Análise Crítica ("Nota histórica —
 * decisão confirmada"): Nota deixa de ser um valor único e mutável por
 * (aluno, disciplina) e passa a ser uma série histórica por
 * (aluno, disciplina, periodoLetivo), preservando o desempenho de períodos
 * letivos anteriores mesmo quando o aluno é repetente ou o período muda.
 *
 * total não é mais um campo livre editável via API: é sempre recalculado
 * por NotaService.recalcular a partir da média de SimuladoAluno.nota
 * (simulados concluídos daquela disciplina, dentro do período letivo),
 * conforme a regra de negócio definida nas Respostas à Análise Crítica
 * ("a plataforma faz a soma das notas apenas dos simulados feitos nela").
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"aluno_id", "disciplina_id", "periodo_letivo"}))
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Nota extends BaseEntity {

    @ManyToOne(optional = false)
    private Aluno aluno;

    @ManyToOne(optional = false)
    private Disciplina disciplina;

    /** Período letivo ao qual esta nota se refere (ex.: "2026.1"). Torna Nota uma série histórica. */
    @Column(name = "periodo_letivo", nullable = false)
    private String periodoLetivo;

    /**
     * Sempre derivado (ver NotaService.recalcular) a partir da média de
     * SimuladoAluno.nota dos simulados concluídos dessa disciplina/período —
     * nunca deveria ser setado diretamente pelo cliente da API.
     */
    private Double total;

    /** Quantidade de simulados concluídos considerados no cálculo de total (transparência do cálculo). */
    private Integer quantidadeSimuladosConsiderados;
}
