package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusSimuladoAluno;

/**
 * Ver item 1.3 da Análise Crítica: registro criado no momento do lançamento
 * do simulado, para todos os alunos elegíveis, com status PENDENTE. Quando o
 * aluno finaliza a tentativa (SimuladoAlunoService.finalizar), o status muda
 * para CONCLUIDO e nota/quantidadeAcertos/tempoGasto passam a ser válidos.
 *
 * tempoGasto é armazenado em segundos e cobre tanto a finalização normal
 * quanto a finalização por esgotamento do tempo limite (ver item 4.2: o
 * simulado do aluno nunca é zerado por timeout — apenas finalizado com o que
 * já havia sido respondido até então).
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class SimuladoAluno extends BaseEntity {

    @ManyToOne
    private Simulado simulado;

    @ManyToOne
    private Aluno aluno;

    private Integer quantidadeAcertos;
    private Double nota;

    /** Tempo total gasto pelo aluno na tentativa, em segundos. */
    private Integer tempoGasto;

    /**
     * true quando a finalização ocorreu por esgotamento do tempo limite do
     * simulado (auto-envio), e não por o aluno confirmar voluntariamente a
     * última questão. Não afeta a nota — apenas informativo/auditoria.
     */
    private Boolean finalizadoPorTempo;

    @Enumerated(EnumType.STRING)
    private StatusSimuladoAluno status;
}
