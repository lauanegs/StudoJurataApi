package studojurata_api.model.turma;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.model.BaseEntity;
import studojurata_api.model.curriculo.Disciplina;
import studojurata_api.model.pessoa.Professor;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class TurmaDisciplina extends BaseEntity {

    @ManyToOne
    private Turma turma;

    @ManyToOne
    private Disciplina disciplina;

    @ManyToOne
    private Professor professor;

    @Enumerated(EnumType.STRING)
    private StatusAtivoInativo status;
}
