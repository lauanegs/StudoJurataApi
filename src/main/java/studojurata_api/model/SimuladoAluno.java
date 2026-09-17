package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusSimuladoAluno;

/**
 * Nasce PENDENTE no lançamento do simulado; nota, acertos e tempoGasto só
 * valem depois de CONCLUIDO. Tempo esgotado finaliza com o que já foi
 * respondido, nunca zera a tentativa.
 */
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

    /** Tempo total gasto pelo aluno na tentativa, em segundos. */
    private Integer tempoGasto;

    /** Finalizado por tempo esgotado. Informativo, não afeta a nota. */
    private Boolean finalizadoPorTempo;

    @Enumerated(EnumType.STRING)
    private StatusSimuladoAluno status;
}
