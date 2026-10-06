package studojurata_api.model.pessoa;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.BaseEntity;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Responsavel extends BaseEntity {
    @OneToOne
    @JoinColumn(unique = true, nullable = false)
    private Pessoa pessoa;
}
