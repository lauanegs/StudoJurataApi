package studojurata_api.model;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Aula extends BaseEntity {

    @ManyToOne
    private TurmaDisciplina turmaDisciplina;

    @ManyToOne
    private PlanoEnsino planoEnsino;

    private Integer cargaHoraria;
    private Integer quantidadeAulas;
    private LocalDate dataPrevista;
    private Integer ordem;
    private String titulo;
    private LocalDate dataPublicacao;
}