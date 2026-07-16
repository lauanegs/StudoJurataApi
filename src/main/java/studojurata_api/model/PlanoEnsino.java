package studojurata_api.model;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusAtivoInativo;

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

    private Integer cargaHoraria;

    /**
     * Correção 2.4 da Terceira Análise Crítica: passou a ser obrigatório.
     * O recálculo automático da Nota do aluno (NotaService.recalcular,
     * chamado por SimuladoAlunoService.finalizar) depende deste campo para
     * agrupar os simulados concluídos por período letivo — antes, um plano
     * de ensino cadastrado sem periodoLetivo fazia esse recálculo ser
     * silenciosamente pulado, e o aluno nunca via a nota da disciplina.
     */
    @Column(nullable = false)
    private String periodoLetivo;

    private String ementa;
    private String objetivoGeral;
    private String metodologia;
    private LocalDate dataInicio;
    private LocalDate dataFim;

    /** Correção 2.11 da Segunda Análise Crítica: era String livre, agora enum. */
    @Enumerated(EnumType.STRING)
    private StatusAtivoInativo status;
}
