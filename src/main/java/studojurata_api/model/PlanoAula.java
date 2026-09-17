package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusPlano;

/**
 * Execução do currículo para uma TurmaDisciplina. Curso e carga horária vêm
 * sempre do PlanoEnsino, sem cópia local que poderia divergir.
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class PlanoAula extends BaseEntity {

    @ManyToOne(optional = false)
    private TurmaDisciplina turmaDisciplina;

    /** 1:1 com PlanoEnsino, garantido também pela constraint unique no banco. */
    @ManyToOne(optional = false)
    @JoinColumn(unique = true)
    private PlanoEnsino planoEnsino;

    @Enumerated(EnumType.STRING)
    private StatusPlano status;
}