package studojurata_api.model;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusPlano;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class PlanoEnsino extends BaseEntity {

    @ManyToOne
    private TurmaDisciplina turmaDisciplina;

    private String titulo;

    /**
     * Vínculo pedido explicitamente: cada Curso pode ter vários Planos de
     * Ensino (um por disciplina do currículo daquele curso). Antes disso
     * existia aqui apenas um campo "curso" (String, @Deprecated) que já
     * havia perdido a função de fonte de verdade para Turma.curso — agora
     * vira a relação de fato com a entidade Curso (ver Curso.java),
     * permitindo listar todos os planos de ensino de um curso
     * (CursoController: GET /cursos/{id}/planos-ensino).
     */
    @ManyToOne(optional = false)
    private Curso curso;

    /**
     * Professor responsável pelo plano de ensino (pedido explícito): antes
     * só existia um vínculo indireto com o professor via turmaDisciplina —
     * que é opcional (um plano genérico, reaproveitável por várias turmas
     * do curso, nunca teve turma vinculada) e semanticamente é sobre
     * "turma+disciplina", não sobre "quem escreveu/responde por este
     * plano". Nullable pra não quebrar planos já existentes sem essa
     * informação (a tela de cadastro passa a pedir, mas dados antigos
     * continuam válidos).
     */
    @ManyToOne
    private Professor professor;

    private Integer cargaHoraria;

    private String ementa;
    private String objetivoGeral;
    private String metodologia;
    private LocalDate dataInicio;
    private LocalDate dataFim;

    /**
     * Correção "matrícula cíclica" (revisão pedagógica): periodoLetivo foi
     * removido — a nota do aluno passou a ser escopada por turma
     * (AlunoTurma.dataInicio), não por calendário fixo (ver Nota.java), e
     * status virou StatusPlano (ATIVO/CONCLUIDO em vez de ATIVO/INATIVO):
     * um plano de ensino não é "desligado", ele conclui o ciclo, e a mesma
     * TurmaDisciplina pode receber um novo plano em seguida.
     */
    @Enumerated(EnumType.STRING)
    private StatusPlano status;
}
