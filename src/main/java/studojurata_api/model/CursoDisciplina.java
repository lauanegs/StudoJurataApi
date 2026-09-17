package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusAtivoInativo;

/**
 * Grade curricular do curso. Restringe quais disciplinas podem ser vinculadas
 * às turmas do curso e compõe Curso.cargaHorariaTotal.
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CursoDisciplina extends BaseEntity {

    @ManyToOne(optional = false)
    private Curso curso;

    @ManyToOne(optional = false)
    private Disciplina disciplina;

    /** Carga horária desta disciplina dentro do curso, em horas. */
    private Integer cargaHoraria;

    @Enumerated(EnumType.STRING)
    private StatusAtivoInativo status;
}
