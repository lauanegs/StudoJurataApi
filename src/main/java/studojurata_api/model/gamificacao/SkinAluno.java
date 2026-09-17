package studojurata_api.model.gamificacao;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.Aluno;
import studojurata_api.model.BaseEntity;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"aluno_id", "skin_id"}))
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class SkinAluno extends BaseEntity {

    @ManyToOne(optional = false)
    private Aluno aluno;

    @ManyToOne(optional = false)
    private Skin skin;

    private LocalDateTime dataAquisicao;

    @Column(nullable = false)
    private Boolean ativa = false;
}
