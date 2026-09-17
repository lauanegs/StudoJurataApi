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

    /** Um plano de ensino por disciplina do currículo do curso. */
    @ManyToOne(optional = false)
    private Curso curso;

    /**
     * Separado do professor de turmaDisciplina, que é opcional (plano genérico
     * não tem turma). Nullable para aceitar planos antigos sem responsável.
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
     * Um plano não é desligado, ele conclui o ciclo; a mesma TurmaDisciplina
     * pode receber um novo plano em seguida.
     */
    @Enumerated(EnumType.STRING)
    private StatusPlano status;
}
