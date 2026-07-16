package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusAtivoInativo;

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

    /** Correção 2.11 da Segunda Análise Crítica: era String livre, agora enum. */
    @Enumerated(EnumType.STRING)
    private StatusAtivoInativo status;
}
