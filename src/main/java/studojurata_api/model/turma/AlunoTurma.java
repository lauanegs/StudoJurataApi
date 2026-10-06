package studojurata_api.model.turma;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusMatricula;
import studojurata_api.model.pessoa.Aluno;
import studojurata_api.model.BaseEntity;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class AlunoTurma extends BaseEntity {
    @ManyToOne(optional = false)
    private Aluno aluno;

    @ManyToOne(optional = false)
    private Turma turma;

    private LocalDate dataInicio;

    private LocalDate dataFim;

    @Enumerated(EnumType.STRING)
    private StatusMatricula status;
}