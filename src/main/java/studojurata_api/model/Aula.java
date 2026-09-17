package studojurata_api.model;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusAtivoInativo;

/**
 * Turma, disciplina e plano de ensino vêm sempre por planoAula, para a cadeia
 * curricular ter um único dono. status permite soft-delete, preservando o
 * histórico pedagógico.
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Aula extends BaseEntity {

    @ManyToOne(optional = false)
    private PlanoAula planoAula;

    /** Quando informado, cargaHoraria é calculada a partir dele (ver AulaService.validar). */
    @ManyToOne
    private HorarioTurma horarioTurma;

    /**
     * Em horas, aceita fração (1.5 = 1h30). Digitada só quando a aula não
     * corresponde a um horário fixo da turma (reposição, aula extra).
     */
    private Double cargaHoraria;

    private LocalDate dataPrevista;

    /** Número/ordem sequencial da aula dentro do plano de aula. */
    private Integer ordem;

    private String titulo;

    /** Data em que a aula realmente foi ministrada (preenchida = aula realizada). */
    private LocalDate dataPublicacao;

    @Column(length = 2000)
    private String observacoes;

    @Enumerated(EnumType.STRING)
    private StatusAtivoInativo status;
}