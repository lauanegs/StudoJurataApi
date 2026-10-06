package studojurata_api.model.simulado;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import studojurata_api.model.BaseEntity;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class QuestaoAluno extends BaseEntity {
    @ManyToOne(optional = false)
    private SimuladoAluno simuladoAluno;

    @ManyToOne(optional = false)
    private Questao questao;

    @ManyToOne
    private Alternativa alternativa;

    @ManyToMany
    @JoinTable(
            name = "questao_aluno_alternativa_verdadeira",
            joinColumns = @JoinColumn(name = "questao_aluno_id"),
            inverseJoinColumns = @JoinColumn(name = "alternativa_id")
    )
    private List<Alternativa> alternativasVerdadeiras = new ArrayList<>();

    private Boolean respondida;

    private Boolean acertou;

    private Integer tempoResposta;
}
