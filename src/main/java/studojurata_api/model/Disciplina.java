package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Disciplina extends BaseEntity {

    private String titulo;
    private Integer cargaHoraria;
    private String status;
}