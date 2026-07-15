package studojurata_api.ia.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Representa uma questão devolvida pelo Gemini (já desserializada do JSON de
 * resposta) antes de ser persistida como Questao/Alternativa. tipo e
 * nivelDificuldade não vêm do modelo — são definidos pelo chamador
 * (GeracaoQuestaoIAService), que já sabe o que pediu.
 */
@Getter
@Setter
public class GeminiQuestaoGeradaDTO {
    private String enunciado;
    private List<AlternativaGeradaDTO> alternativas;
}
