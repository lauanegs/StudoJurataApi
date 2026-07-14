package studojurata_api.model;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.Sexo;
import studojurata_api.model.enums.StatusAtivoInativo;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Pessoa extends BaseEntity {

    private String nome;

    @Column(unique = true, nullable = false, length = 14)
    private String cpf;

    private LocalDate dataNascimento;
    private String telefone;
    private String email;

    @Enumerated(EnumType.STRING)
    private Sexo sexo;

    @Enumerated(EnumType.STRING)
    private StatusAtivoInativo status;
}
