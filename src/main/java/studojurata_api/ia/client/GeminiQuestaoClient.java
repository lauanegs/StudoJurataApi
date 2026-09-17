package studojurata_api.ia.client;

import studojurata_api.ia.dto.GeminiQuestaoGeradaDTO;
import studojurata_api.model.enums.NivelDificuldade;
import studojurata_api.model.enums.TipoQuestao;

import java.util.List;

/**
 * Interface para que GeracaoQuestaoIAService não dependa do transporte HTTP
 * e possa trocar de provedor ou ser testado com um dublê.
 */
public interface GeminiQuestaoClient {

    /**
     * @param idadeAluno idade em anos do aluno destinatário, quando
     *        conhecida (ver Pessoa.dataNascimento) — usada pra adequar
     *        linguagem/complexidade do enunciado; null gera um texto
     *        genérico sem faixa etária específica.
     * @throws GeminiIndisponivelException quando a geração não pôde ser concluída
     *         (chave ausente, timeout, erro HTTP, resposta inesperada).
     */
    List<GeminiQuestaoGeradaDTO> gerarQuestoes(String conteudoTexto, NivelDificuldade nivel, TipoQuestao tipo, int quantidade, Integer idadeAluno);

    /** Identificador do modelo configurado (para auditoria em HistoricoGeracaoIA). */
    String getModelo();
}
