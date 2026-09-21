package studojurata_api.service;

import studojurata_api.model.enums.TipoUsuario;
import studojurata_api.security.UsuarioAutenticado;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.dto.FinalizarSimuladoRequest;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.ia.model.enums.NivelDominio;
import studojurata_api.ia.service.RevisaoConteudoService;
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
import studojurata_api.repository.QuestaoConteudoRepository;
import studojurata_api.repository.SimuladoAlunoRepository;
import studojurata_api.repository.SimuladoQuestaoRepository;
import studojurata_api.security.AlunoAccessGuard;
import studojurata_api.service.gamificacao.PontuacaoAlunoService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SimuladoAlunoService {

    private final SimuladoAlunoRepository repository;
    private final SimuladoQuestaoRepository simuladoQuestaoRepository;
    private final QuestaoAlunoRepository questaoAlunoRepository;
    private final AlternativaRepository alternativaRepository;
    private final QuestaoConteudoRepository questaoConteudoRepository;
    private final NotaService notaService;
    private final AuditLogService auditLogService;
    private final PontuacaoAlunoService pontuacaoAlunoService;
    private final RevisaoConteudoService revisaoConteudoService;
    private final AlunoAccessGuard alunoAccessGuard;
    private final SimuladoService simuladoService;
    private final UsuarioAutenticado usuarioAutenticado;

    /**
     * Listagem escopada: ADMINISTRADOR ve todas as tentativas; PROFESSOR ve as
     * tentativas dos simulados das suas turmas (e dos orfaos preservados por
     * D-C1); ALUNO ve apenas as proprias.
     *
     * <p>Nao filtra status: tentativas PENDENTE continuam na lista.
     */
    public List<SimuladoAluno> listar() {
        var usuario = usuarioAutenticado.atual();

        if (usuario.getTipoUsuario() == TipoUsuario.ADMINISTRADOR) {
            return repository.findAll();
        }

        if (usuario.getTipoUsuario() == TipoUsuario.ALUNO) {
            Long alunoId = usuario.getAluno() != null ? usuario.getAluno().getId() : null;
            return alunoId == null ? List.of() : repository.findByAlunoId(alunoId);
        }

        var simuladoIds = simuladoService.simuladoIdsVisiveis();
        return simuladoIds.isEmpty() ? List.of() : repository.findBySimulado_IdIn(simuladoIds);
    }

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

    /** Id do aluno dono da tentativa; null vira recusa no guard (falha fechado). */
    private Long alunoDaTentativa(SimuladoAluno simuladoAluno) {
        return simuladoAluno.getAluno() != null ? simuladoAluno.getAluno().getId() : null;
    }

    public record ResultadoFinalizacao(SimuladoAluno simuladoAluno, Integer diasProximaRevisao) {}

    private record ResultadoCorrecao(int acertos, double pontuacaoObtida, double pontuacaoTotal) {}

    /**
     * Aceita respostas parciais: questões ausentes contam como em branco (erro).
     * A nota é proporcional à pontuação de cada questão ativa sobre a
     * notaMaxima. Serve também ao auto-envio por tempo esgotado. Uma
     * tentativa CONCLUIDA não pode ser finalizada de novo.
     */
    @Transactional
    public ResultadoFinalizacao finalizar(Long simuladoAlunoId, FinalizarSimuladoRequest request) {
        SimuladoAluno simuladoAluno = buscar(simuladoAlunoId);

        // Autorização ANTES de qualquer efeito: tentativa de outro aluno não
        // grava respostas, não recalcula nota, não concede moedas, não registra
        // revisão e não escreve auditoria.
        alunoAccessGuard.garantirEscritaDoAluno(alunoDaTentativa(simuladoAluno));

        if (simuladoAluno.getStatus() == StatusSimuladoAluno.CONCLUIDO) {
            throw new RegraNegocioException("Esta tentativa já foi finalizada.");
        }
        if (simuladoAluno.getSimulado().getStatus() == StatusSimulado.ENCERRADO) {
            throw new RegraNegocioException("Este simulado já está encerrado.");
        }

        List<SimuladoQuestao> questoes = simuladoQuestaoRepository.findBySimuladoIdAndStatusOrderByOrdem(
                simuladoAluno.getSimulado().getId(), StatusSimuladoQuestao.ATIVA);

        ResultadoCorrecao correcao = corrigirRespostas(simuladoAluno, questoes, request);
        SimuladoAluno salvo = salvarResultado(simuladoAluno, request, correcao, questoes.size());

        recalcularNotaDaDisciplina(salvo);

        // Moedas por concluir, independente da nota: não é bonificação por acerto.
        pontuacaoAlunoService.concederMoedas(salvo.getAluno().getId(),
                PontuacaoAlunoService.MOEDAS_POR_SIMULADO_CONCLUIDO);

        Integer diasProximaRevisao = registrarRevisaoEspacada(salvo.getAluno().getId(), questoes, correcao);

        return new ResultadoFinalizacao(salvo, diasProximaRevisao);
    }

    /**
     * Questão em branco, inclusive V/F com afirmação não julgada, conta como
     * erro em vez de reabrir a tentativa: reabrir deixaria o aluno refazer
     * sabendo o que já tinha respondido.
     */
    private ResultadoCorrecao corrigirRespostas(
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

        return new ResultadoCorrecao(acertos, pontuacaoObtida, pontuacaoTotal);
    }

    private SimuladoAluno salvarResultado(
            SimuladoAluno simuladoAluno, FinalizarSimuladoRequest request, ResultadoCorrecao correcao, int totalQuestoes) {
        Double notaMaxima = simuladoAluno.getSimulado().getNotaMaxima();
        double nota = (notaMaxima != null && correcao.pontuacaoTotal() > 0)
                ? (correcao.pontuacaoObtida() / correcao.pontuacaoTotal()) * notaMaxima
                : correcao.pontuacaoObtida();

        simuladoAluno.setQuantidadeAcertos(correcao.acertos());
        simuladoAluno.setNota(nota);
        simuladoAluno.setTempoGasto(request.getTempoGastoTotal());
        simuladoAluno.setFinalizadoPorTempo(request.isFinalizadoPorTempo());
        simuladoAluno.setStatus(StatusSimuladoAluno.CONCLUIDO);

        SimuladoAluno salvo = repository.save(simuladoAluno);

        auditLogService.registrar("SimuladoAluno", salvo.getId(), AcaoAuditoria.ATUALIZACAO,
                "Finalizado: nota=" + nota + ", acertos=" + correcao.acertos() + "/" + totalQuestoes
                        + (Boolean.TRUE.equals(salvo.getFinalizadoPorTempo()) ? " (por esgotamento do tempo)" : ""));

        return salvo;
    }

    /** Simulado.turma pode ser nula em ESPECIFICO; nesse caso não há como escopar a nota. */
    private void recalcularNotaDaDisciplina(SimuladoAluno salvo) {
        Long disciplinaId = salvo.getSimulado().getDisciplina() != null ? salvo.getSimulado().getDisciplina().getId() : null;
        Long turmaId = salvo.getSimulado().getTurma() != null ? salvo.getSimulado().getTurma().getId() : null;
        if (disciplinaId != null && turmaId != null) {
            notaService.recalcular(salvo.getAluno().getId(), disciplinaId, turmaId);
        } else {
            // Registrado em AuditLog para o Administrador identificar por que o
            // aluno não tem nota nessa disciplina.
            auditLogService.registrar("SimuladoAluno", salvo.getId(), AcaoAuditoria.ATUALIZACAO,
                    "Nota da disciplina NÃO recalculada: disciplina ou turma do simulado ausente "
                            + "(simulado sem turma vinculada). Corrija o cadastro do simulado.");
        }
    }

    /** Qualquer simulado finalizado alimenta a repetição espaçada. */
    private Integer registrarRevisaoEspacada(Long alunoId, List<SimuladoQuestao> questoes, ResultadoCorrecao correcao) {
        double percentualAcerto = correcao.pontuacaoTotal() > 0
                ? (correcao.pontuacaoObtida() / correcao.pontuacaoTotal()) * 100
                : 0;
        LocalDate proximaRevisao = registrarReforcoNosConteudos(alunoId, questoes, percentualAcerto);
        return proximaRevisao != null
                ? (int) ChronoUnit.DAYS.between(LocalDate.now(), proximaRevisao)
                : null;
    }

    /** Mesmos limiares de nivelDesempenho no front (utils/desempenho.ts). */
    private static NivelDominio nivelDominioPeloPercentual(double percentual) {
        if (percentual < 40) return NivelDominio.BAIXO;
        if (percentual < 70) return NivelDominio.MEDIO;
        return NivelDominio.ALTO;
    }

    /**
     * Cada conteúdo tocado avança uma vez por finalização, não uma vez por
     * questão. Retorna a próxima data mais próxima, ou null se nenhum conteúdo
     * tem revisão agendada.
     */
    private LocalDate registrarReforcoNosConteudos(Long alunoId, List<SimuladoQuestao> questoes, double percentualAcerto) {
        List<Long> questaoIds = questoes.stream().map(sq -> sq.getQuestao().getId()).toList();
        if (questaoIds.isEmpty()) return null;

        Set<Long> conteudoPlanoIds = new HashSet<>();
        for (var vinculo : questaoConteudoRepository.findByQuestao_IdIn(questaoIds)) {
            if (vinculo.getConteudoPlano() != null) {
                conteudoPlanoIds.add(vinculo.getConteudoPlano().getId());
            }
        }

        NivelDominio nivel = nivelDominioPeloPercentual(percentualAcerto);
        LocalDate maisProxima = null;
        for (Long conteudoPlanoId : conteudoPlanoIds) {
            var revisao = revisaoConteudoService.registrarReforco(alunoId, conteudoPlanoId, nivel);
            LocalDate data = revisao.getDataProximoReforco();
            if (data != null && (maisProxima == null || data.isBefore(maisProxima))) {
                maisProxima = data;
            }
        }
        return maisProxima;
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
