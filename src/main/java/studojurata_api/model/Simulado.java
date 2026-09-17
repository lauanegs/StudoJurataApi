package studojurata_api.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusSimulado;
import studojurata_api.model.enums.TipoDestinacaoSimulado;

/**
 * Em RASCUNHO o professor monta as questões; ao ser lançado
 * (SimuladoService.lancar) passa a PUBLICADO e cria uma tentativa PENDENTE
 * para cada aluno elegível, conforme tipoDestinacao:
 * - TODOS: todos os alunos com matrícula ATIVA na turma;
 * - ESPECIFICO: apenas os alunos informados no momento do lançamento.
 */
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
