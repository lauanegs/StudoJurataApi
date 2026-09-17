package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusSimuladoQuestao;

/** status permite remover a questão do simulado sem perder as respostas já registradas. */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class SimuladoQuestao extends BaseEntity {

    @ManyToOne(optional = false)
    private Simulado simulado;

    @ManyToOne(optional = false)
    private Questao questao;

    private Integer ordem;
    private Double pontuacao;

    @Enumerated(EnumType.STRING)
    private StatusSimuladoQuestao status;
}
