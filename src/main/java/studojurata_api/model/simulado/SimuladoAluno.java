package studojurata_api.model.simulado;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusSimuladoAluno;
import studojurata_api.model.pessoa.Aluno;
import studojurata_api.model.BaseEntity;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class SimuladoAluno extends BaseEntity {
    @ManyToOne(optional = false)
    private Simulado simulado;

    @ManyToOne(optional = false)
    private Aluno aluno;

    private Integer quantidadeAcertos;
    private Double nota;

    private Integer tempoGasto;

    private Boolean finalizadoPorTempo;

    @Enumerated(EnumType.STRING)
    private StatusSimuladoAluno status;
}
