package studojurata_api.model;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusMatricula;

/**
 * Representa a matrícula de um Aluno em uma Turma.
 *
 * Um mesmo Aluno pode ter múltiplos registros ao longo do tempo (histórico),
 * mas a camada de serviço garante que não existam dois registros com
 * status = ATIVA para o mesmo par (aluno, turma) simultaneamente (ver
 * AlunoTurmaService). Propositalmente não há constraint que impeça um aluno
 * de ter matrículas ATIVAS em turmas diferentes ao mesmo tempo: isso cobre
 * casos como o aluno que continua vinculado à turma do seu curso mas também
 * frequenta outra turma para receber apoio pontual.
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

    /** Preenchida quando a matrícula deixa de estar ATIVA (conclusão, cancelamento ou transferência). */
    private LocalDate dataFim;

    @Enumerated(EnumType.STRING)
    private StatusMatricula status;

    /**
     * Quando esta matrícula é encerrada por transferência (status = TRANSFERIDA),
     * aponta para a nova matrícula criada na turma de destino, permitindo
     * reconstruir a trilha completa do aluno entre turmas.
     */
    @ManyToOne
    private AlunoTurma matriculaDestinoTransferencia;
}