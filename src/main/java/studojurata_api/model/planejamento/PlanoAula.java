package studojurata_api.model.planejamento;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusPlano;
import studojurata_api.model.BaseEntity;
import studojurata_api.model.turma.TurmaDisciplina;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class PlanoAula extends BaseEntity {
    @ManyToOne(optional = false)
    private TurmaDisciplina turmaDisciplina;

    @ManyToOne(optional = false)
    @JoinColumn(unique = true)
    private PlanoEnsino planoEnsino;

    @Enumerated(EnumType.STRING)
    private StatusPlano status;
}