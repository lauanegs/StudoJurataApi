package studojurata_api.model.turma;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusTurma;
import studojurata_api.model.BaseEntity;
import studojurata_api.model.curriculo.Curso;
import studojurata_api.model.institucional.Escola;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Turma extends BaseEntity {
    @ManyToOne(optional = false)
    private Escola escola;

    private String titulo;
    private Integer capacidadeMaxima;

    @ManyToOne(optional = false)
    private Curso curso;

    @Enumerated(EnumType.STRING)
    private StatusTurma status;

    private LocalDate dataInicio;
    private LocalDate dataFim;
}