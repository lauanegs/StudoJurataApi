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
public class AlunoTurma extends BaseEntity {

    @ManyToOne
    private Aluno aluno;

    @ManyToOne
    private Turma turma;

    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String status;
}