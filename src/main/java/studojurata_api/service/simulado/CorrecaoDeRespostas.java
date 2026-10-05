package studojurata_api.service.simulado;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import studojurata_api.dto.simulado.FinalizarSimuladoRequest;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.simulado.Alternativa;
import studojurata_api.model.simulado.Questao;
import studojurata_api.model.simulado.QuestaoAluno;
import studojurata_api.model.simulado.SimuladoAluno;
import studojurata_api.model.simulado.SimuladoQuestao;
import studojurata_api.model.enums.TipoQuestao;
import studojurata_api.repository.simulado.AlternativaRepository;
import studojurata_api.repository.simulado.QuestaoAlunoRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Correção das respostas de uma tentativa: julga cada questão (alternativa
 * única ou verdadeiro/falso), grava a resposta e totaliza acertos e pontuação.
 * Usada na finalização e na conferência imediata de uma questão.
 */
@Component
@RequiredArgsConstructor
public class CorrecaoDeRespostas {

    private final QuestaoAlunoRepository questaoAlunoRepository;
    private final AlternativaRepository alternativaRepository;

    /** respondidas = questões efetivamente respondidas (em branco não conta). */
    public record Resultado(int acertos, int respondidas, double pontuacaoObtida, double pontuacaoTotal) {}

    /** Julga a questão conforme o tipo e preenche a resposta do aluno (alternativa escolhida ou afirmações marcadas). */
    public boolean julgar(Questao questao, FinalizarSimuladoRequest.Item resposta, QuestaoAluno questaoAluno) {
        return questao.getTipo() == TipoQuestao.VERDADEIRO_FALSO
                ? julgarVerdadeiroFalso(questao, resposta, questaoAluno)
                : julgarAlternativas(resposta, questaoAluno);
    }

    /**
     * Questão em branco, inclusive V/F com afirmação não julgada, conta como
     * erro em vez de reabrir a tentativa: reabrir deixaria o aluno refazer
     * sabendo o que já tinha respondido.
     */
    public Resultado corrigir(
            SimuladoAluno simuladoAluno, List<SimuladoQuestao> questoes, FinalizarSimuladoRequest request) {
        Map<Long, FinalizarSimuladoRequest.Item> respostasPorQuestao = new HashMap<>();
        if (request.getRespostas() != null) {
            for (FinalizarSimuladoRequest.Item item : request.getRespostas()) {
                if (item.getQuestaoId() != null) {
                    respostasPorQuestao.put(item.getQuestaoId(), item);
                }
            }
        }

        int acertos = 0;
        int respondidas = 0;
        double pontuacaoObtida = 0d;
        double pontuacaoTotal = 0d;

        for (SimuladoQuestao simuladoQuestao : questoes) {
            Questao questao = simuladoQuestao.getQuestao();
            double pontuacao = simuladoQuestao.getPontuacao() != null ? simuladoQuestao.getPontuacao() : 1d;
            pontuacaoTotal += pontuacao;

            FinalizarSimuladoRequest.Item resposta = respostasPorQuestao.get(questao.getId());

            QuestaoAluno questaoAluno = questaoAlunoRepository
                    .findFirstBySimuladoAlunoIdAndQuestaoId(simuladoAluno.getId(), questao.getId())
                    .orElseGet(QuestaoAluno::new);
            questaoAluno.setSimuladoAluno(simuladoAluno);
            questaoAluno.setQuestao(questao);

            // Questão já conferida durante a prova: vale a resposta gravada, não a do request.
            boolean jaConferida = Boolean.TRUE.equals(questaoAluno.getRespondida());
            boolean acertou = jaConferida
                    ? Boolean.TRUE.equals(questaoAluno.getAcertou())
                    : julgar(questao, resposta, questaoAluno);

            questaoAluno.setAcertou(acertou);
            if (!jaConferida) {
                questaoAluno.setTempoResposta(resposta != null ? resposta.getTempoResposta() : null);
            }
            questaoAlunoRepository.save(questaoAluno);

            if (Boolean.TRUE.equals(questaoAluno.getRespondida())) {
                respondidas++;
            }
            if (acertou) {
                acertos++;
                pontuacaoObtida += pontuacao;
            }
        }

        return new Resultado(acertos, respondidas, pontuacaoObtida, pontuacaoTotal);
    }

    /** Escolha única (tipo ALTERNATIVAS): acerta quem escolheu a alternativa marcada correta=true. */
    private boolean julgarAlternativas(FinalizarSimuladoRequest.Item resposta, QuestaoAluno questaoAluno) {
        questaoAluno.setAlternativasVerdadeiras(List.of());

        if (resposta == null || resposta.getAlternativaId() == null) {
                    questaoAluno.setAlternativa(null);
            questaoAluno.setRespondida(false);
            return false;
        }

        Alternativa alternativaEscolhida = alternativaRepository.findById(resposta.getAlternativaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Alternativa " + resposta.getAlternativaId() + " não encontrada."));
        questaoAluno.setAlternativa(alternativaEscolhida);
        questaoAluno.setRespondida(true);
        return Boolean.TRUE.equals(alternativaEscolhida.getCorreta());
    }

    /**
     * VERDADEIRO_FALSO: cada alternativa da questão é uma afirmação julgada
     * independentemente (correta=true/false é o gabarito — a afirmação É
     * verdadeira ou falsa). O aluno acerta a questão inteira só se marcou
     * TODAS as afirmações certas; uma só errada derruba a questão toda.
     *
     * alternativasVerdadeiras == null no request é o sentinel de "em branco"
     * — diferente de lista vazia, que é uma resposta legítima ("julguei
     * todas as afirmações como Falsas").
     */
    private boolean julgarVerdadeiroFalso(Questao questao, FinalizarSimuladoRequest.Item resposta, QuestaoAluno questaoAluno) {
        List<Alternativa> afirmacoes = alternativaRepository.findByQuestaoIdOrderByOrdem(questao.getId());
        questaoAluno.setAlternativa(null);

        List<Long> idsMarcadosVerdadeiros = resposta != null ? resposta.getAlternativasVerdadeiras() : null;

        if (afirmacoes.isEmpty() || idsMarcadosVerdadeiros == null) {
            // Sem afirmações cadastradas, ou questão deixada em branco.
            questaoAluno.setAlternativasVerdadeiras(List.of());
            questaoAluno.setRespondida(false);
            return false;
        }

        List<Alternativa> marcadasVerdadeiras = afirmacoes.stream()
                .filter(afirmacao -> idsMarcadosVerdadeiros.contains(afirmacao.getId()))
                .toList();
        questaoAluno.setAlternativasVerdadeiras(marcadasVerdadeiras);
        questaoAluno.setRespondida(true);

        return afirmacoes.stream().allMatch(afirmacao ->
                Boolean.TRUE.equals(afirmacao.getCorreta()) == idsMarcadosVerdadeiros.contains(afirmacao.getId()));
    }
}
