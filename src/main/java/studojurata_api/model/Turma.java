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
public class Turma extends BaseEntity {

    private String titulo;
    private Integer capacidadeMaxima;
    private Integer quantidadeAlunos;
    private String status;
    private LocalDate dataInicio;
    private LocalDate dataFim;
}