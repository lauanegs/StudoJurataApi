package studojurata_api.ia.dto;

import lombok.Getter;
import lombok.Setter;

/** Uma alternativa dentro de uma questão gerada pela IA (ver GeminiQuestaoGeradaDTO). */
@Getter
@Setter
public class AlternativaGeradaDTO {
    private String texto;
    private boolean correta;
}
