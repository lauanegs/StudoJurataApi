package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class SimuladoQuestao extends BaseEntity {

    @ManyToOne
    private Simulado simulado;

    @ManyToOne
    private Questao questao;

    private Integer ordem;
    private Double pontuacao;
    private String status;
}