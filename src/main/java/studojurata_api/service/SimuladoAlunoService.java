package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studojurata_api.dto.FinalizarSimuladoRequest;
import studojurata_api.model.Alternativa;
import studojurata_api.model.Questao;
import studojurata_api.model.QuestaoAluno;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.model.SimuladoQuestao;
import studojurata_api.model.enums.StatusSimulado;
import studojurata_api.model.enums.StatusSimuladoAluno;
import studojurata_api.model.enums.StatusSimuladoQuestao;
import studojurata_api.repository.AlternativaRepository;
import studojurata_api.repository.QuestaoAlunoRepository;
import studojurata_api.repository.SimuladoAlunoRepository;
import studojurata_api.repository.SimuladoQuestaoRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SimuladoAlunoService {

    private final SimuladoAlunoRepository repository;
    private final SimuladoQuestaoRepository simuladoQuestaoRepository;
    private final QuestaoAlunoRepository questaoAlunoRepository;
    private final AlternativaRepository alternativaRepository;

    public List<SimuladoAluno> listar() { return repository.findAll(); }

    public SimuladoAluno buscar(Long id) { return repository.findById(id).orElseThrow(); }

    public List<SimuladoAluno> listarPorAluno(Long alunoId) {
        return repository.findByAlunoId(alunoId);
    }

    public List<SimuladoAluno> listarPorSimulado(Long simuladoId) {
        return repository.findBySimuladoId(simuladoId);
    }

    /**
     * Uso administrativo pontual (ex.: correção manual de um registro). O
     * caminho normal de criação é SimuladoService.lancar (item 1.3), que cria
     * um SimuladoAluno PENDENTE por aluno elegível.
     */
    public SimuladoAluno salvar(SimuladoAluno obj) {
        if (obj.getStatus() == null) {
            obj.setStatus(StatusSimuladoAluno.PENDENTE);
        }
        return repository.save(obj);
    }

    public void deletar(Long id) { repository.deleteById(id); }

    /**
     * Finaliza a tentativa do aluno (itens 1.3, 2.4 e 4.2 da Análise
     * Crítica):
     * <ul>
     *   <li>não zera nem descarta o progresso do aluno: aceita uma lista de
     *   respostas parcial;</li>
     *   <li>questões do simulado que não aparecerem em {@code respostas} são
     *   tratadas como deixadas em branco — registradas com alternativa nula
     *   e acertou=false (sugestão adotada para o item 4.2, em aberto na
     *   análise: tratar como erro, igual a uma prova tradicional, sem
     *   bloquear o envio);</li>
     *   <li>calcula quantidadeAcertos e nota (proporcional à pontuação de
     *   cada SimuladoQuestao ativa, sobre a notaMaxima do Simulado);</li>
     *   <li>é idempotente: uma tentativa já CONCLUIDA não pode ser
     *   finalizada de novo, evitando sobrescrever um resultado já
     *   registrado;</li>
     *   <li>serve tanto para a finalização voluntária (aluno confirma a
     *   última questão) quanto para o auto-envio por esgotamento do tempo
     *   limite (request.finalizadoPorTempo = true), conforme o item 4.2: ao
     *   esgotar o tempo o aluno não perde o que já respondeu, apenas recebe
     *   o modal informando que as respostas foram enviadas.</li>
     * </ul>
     */
    @Transactional
    public SimuladoAluno finalizar(Long simuladoAlunoId, FinalizarSimuladoRequest request) {
        SimuladoAluno simuladoAluno = buscar(simuladoAlunoId);

        if (simuladoAluno.getStatus() == StatusSimuladoAluno.CONCLUIDO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta tentativa já foi finalizada.");
        }
        if (simuladoAluno.getSimulado().getStatus() == StatusSimulado.ENCERRADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este simulado já está encerrado.");
        }

        List<SimuladoQuestao> questoes = simuladoQuestaoRepository.findBySimuladoIdAndStatusOrderByOrdem(
                simuladoAluno.getSimulado().getId(), StatusSimuladoQuestao.ATIVA);

        Map<Long, FinalizarSimuladoRequest.Item> respostasPorQuestao = new HashMap<>();
        if (request.getRespostas() != null) {
            for (FinalizarSimuladoRequest.Item item : request.getRespostas()) {
                if (item.getQuestaoId() != null) {
                    respostasPorQuestao.put(item.getQuestaoId(), item);
                }
            }
        }

        int acertos = 0;
        double pontuacaoObtida = 0d;
        double pontuacaoTotal = 0d;

        for (SimuladoQuestao simuladoQuestao : questoes) {
            Questao questao = simuladoQuestao.getQuestao();
            double pontuacao = simuladoQuestao.getPontuacao() != null ? simuladoQuestao.getPontuacao() : 1d;
            pontuacaoTotal += pontuacao;

            FinalizarSimuladoRequest.Item resposta = respostasPorQuestao.get(questao.getId());
            Alternativa alternativaEscolhida = null;
            Integer tempoResposta = null;
            boolean acertou = false;

            if (resposta != null && resposta.getAlternativaId() != null) {
                alternativaEscolhida = alternativaRepository.findById(resposta.getAlternativaId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Alternativa " + resposta.getAlternativaId() + " não encontrada."));
                acertou = Boolean.TRUE.equals(alternativaEscolhida.getCorreta());
                tempoResposta = resposta.getTempoResposta();
            }
            // resposta == null ou alternativaId == null => questão deixada em branco (item 4.2): acertou=false

            if (acertou) {
                acertos++;
                pontuacaoObtida += pontuacao;
            }

            QuestaoAluno questaoAluno = questaoAlunoRepository
                    .findFirstBySimuladoAlunoIdAndQuestaoId(simuladoAluno.getId(), questao.getId())
                    .orElseGet(QuestaoAluno::new);
            questaoAluno.setSimuladoAluno(simuladoAluno);
            questaoAluno.setQuestao(questao);
            questaoAluno.setAlternativa(alternativaEscolhida);
            questaoAluno.setAcertou(acertou);
            questaoAluno.setTempoResposta(tempoResposta);
            questaoAlunoRepository.save(questaoAluno);
        }

        Double notaMaxima = simuladoAluno.getSimulado().getNotaMaxima();
        double nota = (notaMaxima != null && pontuacaoTotal > 0)
                ? (pontuacaoObtida / pontuacaoTotal) * notaMaxima
                : pontuacaoObtida;

        simuladoAluno.setQuantidadeAcertos(acertos);
        simuladoAluno.setNota(nota);
        simuladoAluno.setTempoGasto(request.getTempoGastoTotal());
        simuladoAluno.setFinalizadoPorTempo(request.isFinalizadoPorTempo());
        simuladoAluno.setStatus(StatusSimuladoAluno.CONCLUIDO);

        return repository.save(simuladoAluno);
    }
}
