package studojurata_api.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

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

    private String tipoDestinacao;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    private Integer tempoLimite;
    private Double notaMaxima;
    private Integer quantidadeQuestoes;
    private String status;
}