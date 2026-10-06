package studojurata_api.model.simulado;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusSimulado;
import studojurata_api.model.enums.TipoDestinacaoSimulado;
import studojurata_api.model.BaseEntity;
import studojurata_api.model.curriculo.Disciplina;
import studojurata_api.model.planejamento.PlanoEnsino;
import studojurata_api.model.turma.Turma;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Simulado extends BaseEntity {
    private String titulo;

    @ManyToOne
    private Disciplina disciplina;

    @ManyToOne
    private PlanoEnsino planoEnsino;

    @ManyToOne
    private Turma turma;

    @Enumerated(EnumType.STRING)
    private TipoDestinacaoSimulado tipoDestinacao;

    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    private Integer tempoLimite;
    private Double notaMaxima;
    private Integer quantidadeQuestoes;

    @Enumerated(EnumType.STRING)
    private StatusSimulado status;
}
