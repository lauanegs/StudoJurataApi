package studojurata_api.model.planejamento;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.model.BaseEntity;
import studojurata_api.model.turma.HorarioTurma;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Aula extends BaseEntity {
    @ManyToOne(optional = false)
    private PlanoAula planoAula;

    @ManyToOne
    private HorarioTurma horarioTurma;

    private Double cargaHoraria;

    private LocalDate dataPrevista;

    private Integer ordem;

    private String titulo;

    private LocalDate dataPublicacao;

    @Column(length = 2000)
    private String observacoes;

    @Enumerated(EnumType.STRING)
    private StatusAtivoInativo status;
}