package studojurata_api.model;

import java.time.LocalTime;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.DiaSemana;

/**
 * Correção solicitada após a Terceira Análise Crítica: cada Turma passa a
 * ter um horário semanal. Uma turma normalmente tem aula em mais de um dia
 * da semana (ex.: segunda e quarta, 19h-21h) — por isso é uma entidade
 * própria (1 Turma : N HorarioTurma), não um único campo de horário em
 * Turma, que só conseguiria representar um dia/intervalo por vez.
 */
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
