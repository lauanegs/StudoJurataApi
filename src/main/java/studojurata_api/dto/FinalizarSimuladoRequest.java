package studojurata_api.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Corpo de requisição para finalizar a tentativa de um aluno em um simulado
 * (SimuladoAluno). Ver itens 2.4 e 4.2 da Análise Crítica:
 * - respostas traz uma entrada por questão respondida; questões do simulado
 *   que não aparecerem na lista são tratadas como deixadas em branco (ver
 *   item 4.2 — não bloqueiam a finalização, contam como erro);
 * - tempoGastoTotal é o tempo total da tentativa, em segundos, preenchido
 *   tanto na finalização voluntária quanto na finalização automática por
 *   esgotamento do tempo limite (finalizadoPorTempo = true nesse caso).
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
        /** Nula quando a questão foi deixada em branco. */
        private Long alternativaId;
        private Integer tempoResposta;
    }
}
