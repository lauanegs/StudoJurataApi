package studojurata_api.model.planejamento;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusPlano;
import studojurata_api.model.BaseEntity;
import studojurata_api.model.curriculo.Curso;
import studojurata_api.model.pessoa.Professor;
import studojurata_api.model.turma.TurmaDisciplina;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class PlanoEnsino extends BaseEntity {
    @ManyToOne
    private TurmaDisciplina turmaDisciplina;

    @ManyToOne(optional = false)
    private Curso curso;

    @ManyToOne
    private Professor professor;

    private Integer cargaHoraria;

    @Column(columnDefinition = "TEXT")

    private String ementa;
    @Column(columnDefinition = "TEXT")
    private String objetivoGeral;
    @Column(columnDefinition = "TEXT")
    private String metodologia;
    private LocalDate dataInicio;
    private LocalDate dataFim;

    @Enumerated(EnumType.STRING)
    private StatusPlano status;
}
