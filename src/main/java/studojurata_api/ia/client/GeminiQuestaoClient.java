package studojurata_api.ia.client;

import studojurata_api.ia.dto.GeminiQuestaoGeradaDTO;
import studojurata_api.model.enums.NivelDificuldade;
import studojurata_api.model.enums.TipoQuestao;

import java.util.List;

public interface GeminiQuestaoClient {
    List<GeminiQuestaoGeradaDTO> gerarQuestoes(String conteudoTexto, NivelDificuldade nivel, TipoQuestao tipo, int quantidade, Integer idadeAluno);

    String getModelo();
}
