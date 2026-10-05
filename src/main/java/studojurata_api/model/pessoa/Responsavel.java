package studojurata_api.model.pessoa;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.BaseEntity;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Responsavel extends BaseEntity {

    /**
     * 1 Pessoa = no máximo 1 Responsavel. Os alunos ficam em ResponsavelAluno
     * (N:N, com parentesco).
     */
    @OneToOne
    @JoinColumn(unique = true, nullable = false)
    private Pessoa pessoa;
}
