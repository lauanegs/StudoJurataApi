package studojurata_api.ia.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GeminiQuestaoGeradaDTO {
    private String enunciado;
    private List<AlternativaGeradaDTO> alternativas;
}
