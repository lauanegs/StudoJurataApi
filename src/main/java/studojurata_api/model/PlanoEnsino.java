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
     * @deprecated a fonte de verdade do curso passou a ser Turma.curso (ver
     * correção da "gambiarra" de troca de turma na Segunda Análise Crítica) —
     * mantido aqui apenas por compatibilidade, não usar em código novo.
     */
    @Deprecated
    private String curso;

    private Integer cargaHoraria;
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
