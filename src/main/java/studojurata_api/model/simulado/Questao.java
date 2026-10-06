package studojurata_api.model.simulado;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.NivelDificuldade;
import studojurata_api.model.enums.OrigemQuestao;
import studojurata_api.model.enums.StatusQuestao;
import studojurata_api.model.enums.TipoQuestao;
import studojurata_api.model.BaseEntity;
import studojurata_api.model.curriculo.Disciplina;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Questao extends BaseEntity {
    @Column(columnDefinition = "TEXT")

    private String enunciado;

    @Enumerated(EnumType.STRING)
    private TipoQuestao tipo;

    @ManyToOne
    private Disciplina disciplina;

    @Enumerated(EnumType.STRING)
    private NivelDificuldade nivelDificuldade;

    @Enumerated(EnumType.STRING)
    private OrigemQuestao origem;

    @Enumerated(EnumType.STRING)
    private StatusQuestao status;
}
