package studojurata_api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    /**
     * 1 Pessoa = no máximo 1 Usuario (login).
     */
    @OneToOne
    @JoinColumn(unique = true, nullable = false)
    private Pessoa pessoa;

    @Column(unique = true, nullable = false)
    private String username;

    /**
     * Sempre armazenada com hash (BCrypt), nunca em texto puro.
     * Nunca é serializada nas respostas da API.
     */
    @JsonIgnore
    private String senha;

    /**
     * Papel do usuário, usado tanto para regra de negócio quanto para
     * autorização (Spring Security). Substitui o antigo tipoUsuario em String livre.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoUsuario tipoUsuario;

    @Enumerated(EnumType.STRING)
    private StatusAtivoInativo status;

    /**
     * Preenchido apenas quando tipoUsuario = ALUNO. Referência explícita ao
     * perfil de negócio representado por este login, em vez de depender
     * implicitamente da Pessoa para descobrir o perfil.
     */
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
