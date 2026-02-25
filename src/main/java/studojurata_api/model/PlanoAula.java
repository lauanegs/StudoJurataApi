package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class PlanoAula extends BaseEntity {

    @ManyToOne
    private TurmaDisciplina turmaDisciplina;

    @ManyToOne
    private PlanoEnsino planoEnsino;

    private Integer cargaHoraria;
    private String curso;
    private String periodoLetivo;
    private String status;
}