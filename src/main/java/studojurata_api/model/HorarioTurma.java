package studojurata_api.model;

import java.time.LocalTime;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.DiaSemana;

/** Entidade própria porque uma turma costuma ter aula em mais de um dia da semana. */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class HorarioTurma extends BaseEntity {

    @ManyToOne(optional = false)
    private Turma turma;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiaSemana diaSemana;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFim;
}
