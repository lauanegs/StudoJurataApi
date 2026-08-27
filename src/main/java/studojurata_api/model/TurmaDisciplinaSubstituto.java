package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusAtivoInativo;

/**
 * Professor(es) que também podem ministrar/registrar aula na disciplina de
 * uma turma, além do titular (TurmaDisciplina.professor) — cobre o caso real
 * da escola de um segundo professor substituir/co-lecionar a mesma matéria.
 * Não duplica Plano de Ensino/Plano de Aula: eles continuam únicos por
 * TurmaDisciplina, compartilhados entre titular e substitutos.
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class TurmaDisciplinaSubstituto extends BaseEntity {

    @ManyToOne
    private TurmaDisciplina turmaDisciplina;

    @ManyToOne
    private Professor professor;

    @Enumerated(EnumType.STRING)
    private StatusAtivoInativo status;
}
