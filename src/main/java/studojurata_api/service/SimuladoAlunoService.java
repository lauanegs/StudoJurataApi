package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.dto.FinalizarSimuladoRequest;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.model.Alternativa;
import studojurata_api.model.Questao;
import studojurata_api.model.QuestaoAluno;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.model.SimuladoQuestao;
import studojurata_api.model.enums.AcaoAuditoria;
import studojurata_api.model.enums.StatusSimulado;
import studojurata_api.model.enums.StatusSimuladoAluno;
import studojurata_api.model.enums.StatusSimuladoQuestao;
import studojurata_api.model.enums.TipoQuestao;
import studojurata_api.repository.AlternativaRepository;
import studojurata_api.repository.QuestaoAlunoRepository;
import studojurata_api.repository.SimuladoAlunoRepository;
import studojurata_api.repository.SimuladoQuestaoRepository;
import studojurata_api.service.gamificacao.PontuacaoAlunoService;

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
    private final NotaService notaService;
    private final AuditLogService auditLogService;
    private final PontuacaoAlunoService pontuacaoAlunoService;

    public List<SimuladoAluno> listar() { return repository.findAll(); }

    public SimuladoAluno buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("SimuladoAluno " + id + " não encontrado."));
    }

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
            throw new RegraNegocioException("Esta tentativa já foi finalizada.");
        }
        if (simuladoAluno.getSimulado().getStatus() == StatusSimulado.ENCERRADO) {
            throw new RegraNegocioException("Este simulado já está encerrado.");
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

        // Item 4.2 (mantido, decisão revista com o usuário): questão em branco
        // — incluindo VERDADEIRO_FALSO com alguma afirmação não julgada — não
        // bloqueia a finalização nem reinicia a tentativa; ela só conta como
        // erro na correção. Deixar o back reabrir a tentativa deixaria o aluno
        // refazer o simulado já sabendo quais respostas confirmou como certas
        // ou erradas antes do tempo acabar — pior do que simplesmente errar
        // as questões que ficaram sem resposta.

        int acertos = 0;
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

            boolean acertou = questao.getTipo() == TipoQuestao.VERDADEIRO_FALSO
                    ? julgarVerdadeiroFalso(questao, resposta, questaoAluno)
                    : julgarAlternativas(resposta, questaoAluno);

            questaoAluno.setAcertou(acertou);
            questaoAluno.setTempoResposta(resposta != null ? resposta.getTempoResposta() : null);
            questaoAlunoRepository.save(questaoAluno);

            if (acertou) {
                acertos++;
                pontuacaoObtida += pontuacao;
            }
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

        SimuladoAluno salvo = repository.save(simuladoAluno);

        auditLogService.registrar("SimuladoAluno", salvo.getId(), AcaoAuditoria.ATUALIZACAO,
                "Finalizado: nota=" + nota + ", acertos=" + acertos + "/" + questoes.size()
                        + (Boolean.TRUE.equals(salvo.getFinalizadoPorTempo()) ? " (por esgotamento do tempo)" : ""));

        // Correção 1.2/2.13: Nota da disciplina é sempre recalculada (derivada) a
        // partir dos simulados concluídos, nunca setada diretamente.
        // Correção "matrícula cíclica": o escopo passou de periodoLetivo
        // (calendário) para turma — Simulado.turma é obrigatório quando
        // tipoDestinacao = TODOS, mas pode ser nulo em ESPECIFICO.
        Long disciplinaId = salvo.getSimulado().getDisciplina() != null ? salvo.getSimulado().getDisciplina().getId() : null;
        Long turmaId = salvo.getSimulado().getTurma() != null ? salvo.getSimulado().getTurma().getId() : null;
        if (disciplinaId != null && turmaId != null) {
            notaService.recalcular(salvo.getAluno().getId(), disciplinaId, turmaId);
        } else {
            // Sem disciplina ou sem turma vinculada ao simulado (ex.: simulado
            // ESPECIFICO sem turma) não há como escopar a nota — o recálculo é
            // pulado e registrado em AuditLog para o Administrador identificar,
            // em vez de o aluno simplesmente nunca ver a nota da disciplina,
            // sem explicação.
            auditLogService.registrar("SimuladoAluno", salvo.getId(), AcaoAuditoria.ATUALIZACAO,
                    "Nota da disciplina NÃO recalculada: disciplina ou turma do simulado ausente "
                            + "(simulado sem turma vinculada). Corrija o cadastro do simulado.");
        }

        // Correção 8.1/8.2: moeda concedida sempre por concluir o simulado,
        // independente da nota obtida (equidade — não é bonificação por acerto).
        pontuacaoAlunoService.concederMoedas(salvo.getAluno().getId(),
                PontuacaoAlunoService.MOEDAS_POR_SIMULADO_CONCLUIDO);

        return salvo;
    }

    /** Escolha única (tipo ALTERNATIVAS): acerta quem escolheu a alternativa marcada correta=true. */
    private boolean julgarAlternativas(FinalizarSimuladoRequest.Item resposta, QuestaoAluno questaoAluno) {
        questaoAluno.setAlternativasVerdadeiras(List.of());

        if (resposta == null || resposta.getAlternativaId() == null) {
            // Questão deixada em branco (item 4.2): acertou=false.
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
     * TODAS as afirmações certas; uma só errada derruba a questão toda,
     * igual a uma prova tradicional (decisão confirmada com o usuário).
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
            // Sem afirmações cadastradas, ou questão deixada em branco (item 4.2).
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
