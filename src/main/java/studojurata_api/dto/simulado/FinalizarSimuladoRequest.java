package studojurata_api.dto.simulado;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

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
        private Long alternativaId;
        private List<Long> alternativasVerdadeiras;
        private Integer tempoResposta;
    }
}
