package studojurata_api.model.pessoa;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.model.enums.TipoUsuario;
import studojurata_api.model.BaseEntity;
import studojurata_api.model.institucional.Escola;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Usuario extends BaseEntity {
    @ManyToOne(optional = false)
    private Escola escola;

    @OneToOne
    @JoinColumn(unique = true, nullable = false)
    private Pessoa pessoa;

    @Column(unique = true, nullable = false)
    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoUsuario tipoUsuario;

    @Enumerated(EnumType.STRING)
    private StatusAtivoInativo status;

    @OneToOne
    @JoinColumn(unique = true)
    private Aluno aluno;

    @OneToOne
    @JoinColumn(unique = true)
    private Professor professor;
}
