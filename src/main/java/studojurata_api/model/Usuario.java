package studojurata_api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.model.enums.TipoUsuario;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Usuario extends BaseEntity {

    @ManyToOne(optional = false)
    private Escola escola;

    /**
     * 1 Pessoa = no máximo 1 Usuario (login).
     */
    @OneToOne
    @JoinColumn(unique = true, nullable = false)
    private Pessoa pessoa;

    @Column(unique = true, nullable = false)
    private String username;

    /**
     * Hash BCrypt. WRITE_ONLY e não @JsonIgnore: a senha precisa ser aceita na
     * entrada, só nunca serializada na resposta.
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoUsuario tipoUsuario;

    @Enumerated(EnumType.STRING)
    private StatusAtivoInativo status;

    /** Preenchido apenas quando tipoUsuario = ALUNO. */
    @OneToOne
    @JoinColumn(unique = true)
    private Aluno aluno;

    /**
     * Preenchido apenas quando tipoUsuario = PROFESSOR.
     */
    @OneToOne
    @JoinColumn(unique = true)
    private Professor professor;
}
