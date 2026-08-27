package studojurata_api.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusSimulado;
import studojurata_api.model.enums.TipoDestinacaoSimulado;

/**
 * Ver item 1.3 da Análise Crítica: o Simulado passa por um ciclo de vida
 * explícito (status). Enquanto RASCUNHO, o professor monta as questões
 * (SimuladoQuestao); ao ser lançado (SimuladoService.lancar), passa a
 * PUBLICADO e são criados os registros SimuladoAluno (status PENDENTE) para
 * todos os alunos elegíveis, conforme tipoDestinacao:
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
