package studojurata_api.model;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusTurma;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Turma extends BaseEntity {

    /** Correção 9.1 (Escola/tenant): toda turma pertence a uma escola. */
    @ManyToOne(optional = false)
    private Escola escola;

    private String titulo;
    private Integer capacidadeMaxima;

    /**
     * Correção da "gambiarra" de troca de turma (Segunda Análise Crítica):
     * curso passa a ser um atributo direto da Turma, não mais derivado
     * indiretamente de PlanoEnsino.curso via TurmaDisciplina (onde nada
     * garantia que todas as disciplinas da mesma turma apontassem para o
     * mesmo curso). Não existe mais o conceito de "turma oficial vs. de
     * apoio": toda turma é igualmente oficial, e o aluno sempre segue o
     * curso vinculado à turma em que está matriculado (ver AlunoTurma).
     */
    private String curso;

    /*
     * quantidadeAlunos foi removido: era um campo persistido e redundante,
     * sujeito a desincronização em relação às matrículas ativas reais.
     * A contagem de alunos ativos agora é sempre derivada via
     * AlunoTurmaRepository.countByTurmaIdAndStatus(turmaId, ATIVA)
     * (ver TurmaService.contarAlunosAtivos / AlunoTurmaService), nunca
     * armazenada como fonte de verdade.
     */

    @Enumerated(EnumType.STRING)
    private StatusTurma status;

    private LocalDate dataInicio;
    private LocalDate dataFim;
}