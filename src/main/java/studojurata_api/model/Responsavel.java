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
     * 1 Pessoa = no máximo 1 Responsavel.
     * O vínculo com os alunos deixou de ser uma FK direta (1:N rígido) e
     * passou a ser resolvido pela tabela associativa ResponsavelAluno,
     * permitindo um responsável ter vários alunos e um aluno ter vários
     * responsáveis, cada um com seu parentesco.
     */
    @OneToOne
    @JoinColumn(unique = true, nullable = false)
    private Pessoa pessoa;
}
