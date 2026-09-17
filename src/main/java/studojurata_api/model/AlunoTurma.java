package studojurata_api.model;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusMatricula;

/**
 * Matrícula de um Aluno em uma Turma. A unicidade de matrícula ATIVA é por
 * par (aluno, turma), garantida em AlunoTurmaService: um aluno pode estar
 * ativo em várias turmas ao mesmo tempo.
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class AlunoTurma extends BaseEntity {

    @ManyToOne(optional = false)
    private Aluno aluno;

    @ManyToOne(optional = false)
    private Turma turma;

    private LocalDate dataInicio;

    /** Preenchida quando a matrícula deixa de estar ATIVA (conclusão ou cancelamento). */
    private LocalDate dataFim;

    @Enumerated(EnumType.STRING)
    private StatusMatricula status;
}