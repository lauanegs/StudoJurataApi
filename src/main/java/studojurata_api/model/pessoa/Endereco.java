package studojurata_api.model.pessoa;

import jakarta.persistence.Entity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import studojurata_api.model.BaseEntity;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Endereco extends BaseEntity {

    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;

    /** UF, 2 letras. */
    private String estado;
}
