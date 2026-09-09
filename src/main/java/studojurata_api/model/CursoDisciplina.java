package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusAtivoInativo;

/**
 * Grade curricular (pedido explícito): quais disciplinas compõem um Curso,
 * cada uma com sua própria carga horária. Antes disso a disciplina só
 * aparecia indiretamente ligada a um curso, através de alguma Turma já
 * criada (via TurmaDisciplina) — sem essa entidade, era possível vincular
 * qualquer disciplina da escola a uma turma, mesmo uma que não fizesse
 * parte do currículo do curso.
 *
 * Curso.cargaHorariaTotal deixa de ser digitado à parte no cadastro do
 * curso e passa a ser a soma das cargas horárias ativas desta grade (ver
 * CursoDisciplinaService.recalcularCargaHorariaTotal).
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
