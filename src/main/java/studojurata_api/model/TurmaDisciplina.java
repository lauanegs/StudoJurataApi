package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

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

    private String status;
}