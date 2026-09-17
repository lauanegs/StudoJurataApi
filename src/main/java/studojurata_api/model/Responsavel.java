package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

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
