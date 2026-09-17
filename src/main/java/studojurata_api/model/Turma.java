package studojurata_api.model;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusTurma;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Turma extends BaseEntity {

    @ManyToOne(optional = false)
    private Escola escola;

    private String titulo;
    private Integer capacidadeMaxima;

    /**
     * O curso é da turma, não derivado das disciplinas: o aluno segue o curso
     * da turma em que está matriculado.
     */
    @ManyToOne(optional = false)
    private Curso curso;

    @Enumerated(EnumType.STRING)
    private StatusTurma status;

    private LocalDate dataInicio;
    private LocalDate dataFim;
}