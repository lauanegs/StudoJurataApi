package studojurata_api.model.curriculo;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.model.BaseEntity;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CursoDisciplina extends BaseEntity {
    @ManyToOne(optional = false)
    private Curso curso;

    @ManyToOne(optional = false)
    private Disciplina disciplina;

    private Integer cargaHoraria;

    @Enumerated(EnumType.STRING)
    private StatusAtivoInativo status;
}
