package studojurata_api.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Usuario extends BaseEntity {

    @ManyToOne
    private Pessoa pessoa;

    private String username;
    private String senha;
    private String tipoUsuario;
    private String status;
}