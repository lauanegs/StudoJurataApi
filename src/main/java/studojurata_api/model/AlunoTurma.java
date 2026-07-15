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
 * AlunoTurmaService). Um aluno pode, sim, ter matrículas ATIVAS em turmas
 * diferentes ao mesmo tempo — matricular-se em duas ou mais turmas
 * simultaneamente é um cenário normal e sempre suportado, não uma exceção.
 * A regra de unicidade é sempre por par (aluno, turma), nunca "1 turma ativa
 * por aluno no sistema todo".
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