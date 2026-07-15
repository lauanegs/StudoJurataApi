package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusSimuladoQuestao;

/**
 * Vínculo entre Simulado e Questao. status permite remover (soft-delete) uma
 * questão de um simulado sem perder o histórico de respostas já registradas
 * contra ela (ver Casos Extremos: "Conteudo removido" /
 * "Plano de ensino alterado após simulados já realizados").
 */
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

    @Enumerated(EnumType.STRING)
    private StatusSimuladoQuestao status;
}
