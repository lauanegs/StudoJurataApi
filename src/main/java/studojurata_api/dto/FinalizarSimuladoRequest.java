package studojurata_api.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Questões do simulado ausentes em respostas contam como deixadas em branco
 * (erro), sem bloquear a finalização. tempoGastoTotal é em segundos e vem
 * preenchido também na finalização automática por tempo esgotado.
 */
@Getter
@Setter
public class FinalizarSimuladoRequest {

    private List<Item> respostas;
    private Integer tempoGastoTotal;
    private boolean finalizadoPorTempo;

    @Getter
    @Setter
    public static class Item {
        private Long questaoId;
        /** Nula quando a questão foi deixada em branco. Ignorado para questão VERDADEIRO_FALSO. */
        private Long alternativaId;
        /**
         * Só usado para questão VERDADEIRO_FALSO: ids das alternativas
         * (afirmações) que o aluno marcou como Verdadeiras — as demais
         * alternativas da questão contam como marcadas Falsas. Lista vazia
         * ou nula = questão deixada em branco (todas contam como erradas).
         */
        private List<Long> alternativasVerdadeiras;
        private Integer tempoResposta;
    }
}
