package studojurata_api.model.institucional;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.BaseEntity;
import studojurata_api.model.pessoa.Usuario;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Evento extends BaseEntity {

    @Column(nullable = false)
    private String titulo;

    @Column(length = 2000)
    private String descricao;

    @Column(nullable = false)
    private LocalDateTime dataHorario;

    @Column(nullable = false)
    private Boolean concluido = false;

    /**
     * Usuario (Administrador) que criou o evento. Nao e serializado: a leitura de
     * eventos e de qualquer usuario autenticado, e o Usuario carrega Pessoa (CPF,
     * contato, endereco) — quem criou nao e dado de quem consulta.
     */
    @JsonIgnore
    @ManyToOne
    private Usuario criadoPor;
}
