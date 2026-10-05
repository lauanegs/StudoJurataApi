package studojurata_api.service.simulado;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.dto.simulado.FinalizarSimuladoRequest;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.model.simulado.Alternativa;
import studojurata_api.model.simulado.Questao;
import studojurata_api.model.simulado.QuestaoAluno;
import studojurata_api.model.simulado.SimuladoAluno;
import studojurata_api.model.simulado.SimuladoQuestao;
import studojurata_api.model.enums.AcaoAuditoria;
import studojurata_api.model.enums.StatusSimulado;
import studojurata_api.model.enums.StatusSimuladoAluno;
import studojurata_api.model.enums.StatusSimuladoQuestao;
import studojurata_api.model.enums.TipoUsuario;
import studojurata_api.repository.simulado.AlternativaRepository;
import studojurata_api.repository.simulado.QuestaoAlunoRepository;
import studojurata_api.repository.simulado.SimuladoAlunoRepository;
import studojurata_api.repository.simulado.SimuladoQuestaoRepository;
import studojurata_api.security.AlunoAccessGuard;
import studojurata_api.security.UsuarioAutenticado;

import java.util.List;
import java.util.Map;
import studojurata_api.service.auditoria.AuditLogService;

@Service
@RequiredArgsConstructor
public class SimuladoAlunoService {

    private final SimuladoAlunoRepository repository;
    private final SimuladoQuestaoRepository simuladoQuestaoRepository;
    private final QuestaoAlunoRepository questaoAlunoRepository;
    private final AlternativaRepository alternativaRepository;
    private final AuditLogService auditLogService;
    private final AlunoAccessGuard alunoAccessGuard;
    private final SimuladoService simuladoService;
    private final UsuarioAutenticado usuarioAutenticado;
    private final CorrecaoDeRespostas correcaoDeRespostas;
    private final EfeitosDaFinalizacao efeitosDaFinalizacao;

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

    /**
     * Questoes da tentativa para o proprio aluno (ou ADMIN): vinculos ATIVA do
     * simulado, na ordem, com alternativas em lote e sem gabarito enquanto a
     * tentativa estiver PENDENTE. Professor recebe 403 neste endpoint.
     *
     * <p>Nao usa nada do que o cliente envia: a correcao continua sendo feita no
     * servidor em {@link #finalizar}.
     */
    @Transactional(readOnly = true)
    public studojurata_api.dto.simulado.QuestaoDaTentativaDTO questoesDaTentativa(Long simuladoAlunoId) {
        SimuladoAluno tentativa = buscar(simuladoAlunoId);
        // Em andamento, a prova é só do aluno; depois de concluída, o professor
        // que leciona para ele também lê a correção (questões + gabarito).
        if (tentativa.getStatus() == StatusSimuladoAluno.CONCLUIDO) {
            alunoAccessGuard.garantir(alunoDaTentativa(tentativa));
        } else {
            alunoAccessGuard.garantirDonoOuAdministrador(alunoDaTentativa(tentativa));
        }

        List<SimuladoQuestao> vinculos = simuladoQuestaoRepository.findBySimuladoIdAndStatusOrderByOrdem(
                tentativa.getSimulado().getId(), StatusSimuladoQuestao.ATIVA);

        List<Long> questaoIds = vinculos.stream()
                .map(vinculo -> vinculo.getQuestao().getId())
                .filter(java.util.Objects::nonNull)
                .toList();

        Map<Long, List<Alternativa>> alternativasPorQuestao = questaoIds.isEmpty()
                ? Map.of()
                : alternativaRepository.findByQuestao_IdIn(questaoIds).stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                alternativa -> alternativa.getQuestao().getId(),
                                java.util.LinkedHashMap::new,
                                java.util.stream.Collectors.toList()));

        boolean concluida = tentativa.getStatus() == StatusSimuladoAluno.CONCLUIDO;

        List<studojurata_api.dto.simulado.QuestaoDaTentativaDTO.Item> itens = new java.util.ArrayList<>();
        Map<Long, Boolean> gabarito = new java.util.LinkedHashMap<>();

        for (SimuladoQuestao vinculo : vinculos) {
            Questao questao = vinculo.getQuestao();
            List<Alternativa> alternativas = alternativasPorQuestao.getOrDefault(questao.getId(), List.of());

            List<studojurata_api.dto.simulado.AlternativaDaTentativaDTO> alternativasDTO = alternativas.stream()
                    .sorted(java.util.Comparator.comparing(Alternativa::getOrdem,
                            java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                    .map(alternativa -> new studojurata_api.dto.simulado.AlternativaDaTentativaDTO(
                            alternativa.getId(), alternativa.getTexto(), alternativa.getOrdem()))
                    .toList();

            itens.add(new studojurata_api.dto.simulado.QuestaoDaTentativaDTO.Item(
                    questao.getId(), questao.getEnunciado(), questao.getTipo(), alternativasDTO));

            if (concluida) {
                alternativas.forEach(alternativa ->
                        gabarito.put(alternativa.getId(), Boolean.TRUE.equals(alternativa.getCorreta())));
            }
        }

        var dto = new studojurata_api.dto.simulado.QuestaoDaTentativaDTO();
        dto.setSimuladoAlunoId(tentativa.getId());
        dto.setStatus(tentativa.getStatus());
        dto.setQuestoes(itens);
        dto.setGabarito(concluida ? gabarito : null);
        return dto;
    }

    /** Resultado da conferência imediata: se acertou e quais alternativas da questão são as corretas. */
    public record ConferenciaQuestao(boolean acertou, List<Long> alternativasCorretasIds) {}

    /**
     * Feedback imediato durante a prova. A resposta é gravada e fica travada:
     * uma questão só pode ser conferida uma vez (senão o aluno testaria
     * alternativa por alternativa) e a finalização respeita o que foi gravado.
     */
    @Transactional
    public ConferenciaQuestao conferirQuestao(Long simuladoAlunoId, FinalizarSimuladoRequest.Item resposta) {
        SimuladoAluno simuladoAluno = buscar(simuladoAlunoId);
        alunoAccessGuard.garantirEscritaDoAluno(alunoDaTentativa(simuladoAluno));

        if (simuladoAluno.getStatus() == StatusSimuladoAluno.CONCLUIDO) {
            throw new RegraNegocioException("Esta tentativa já foi finalizada.");
        }
        if (simuladoAluno.getSimulado().getStatus() == StatusSimulado.ENCERRADO) {
            throw new RegraNegocioException("Este simulado já está encerrado.");
        }

        Questao questao = simuladoQuestaoRepository
                .findBySimuladoIdAndStatusOrderByOrdem(simuladoAluno.getSimulado().getId(), StatusSimuladoQuestao.ATIVA)
                .stream()
                .map(SimuladoQuestao::getQuestao)
                .filter(candidata -> candidata.getId().equals(resposta.getQuestaoId()))
                .findFirst()
                .orElseThrow(() -> new RegraNegocioException("Esta questão não pertence ao simulado."));

        QuestaoAluno questaoAluno = questaoAlunoRepository
                .findFirstBySimuladoAlunoIdAndQuestaoId(simuladoAluno.getId(), questao.getId())
                .orElseGet(QuestaoAluno::new);
        if (Boolean.TRUE.equals(questaoAluno.getRespondida())) {
            throw new RegraNegocioException("Esta questão já foi respondida.");
        }

        questaoAluno.setSimuladoAluno(simuladoAluno);
        questaoAluno.setQuestao(questao);
        boolean acertou = correcaoDeRespostas.julgar(questao, resposta, questaoAluno);
        if (!Boolean.TRUE.equals(questaoAluno.getRespondida())) {
            throw new RegraNegocioException("Selecione uma resposta antes de confirmar.");
        }
        questaoAluno.setAcertou(acertou);
        questaoAluno.setTempoResposta(resposta.getTempoResposta());
        questaoAlunoRepository.save(questaoAluno);

        List<Long> corretas = alternativaRepository.findByQuestaoIdOrderByOrdem(questao.getId()).stream()
                .filter(alternativa -> Boolean.TRUE.equals(alternativa.getCorreta()))
                .map(Alternativa::getId)
                .toList();
        return new ConferenciaQuestao(acertou, corretas);
    }

    public record ResultadoFinalizacao(SimuladoAluno simuladoAluno, Integer diasProximaRevisao) {}

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

        CorrecaoDeRespostas.Resultado correcao = correcaoDeRespostas.corrigir(simuladoAluno, questoes, request);
        SimuladoAluno salvo = salvarResultado(simuladoAluno, request, correcao, questoes.size());

        Integer diasProximaRevisao = efeitosDaFinalizacao.aplicar(salvo, correcao, questoes);

        return new ResultadoFinalizacao(salvo, diasProximaRevisao);
    }

    private SimuladoAluno salvarResultado(
            SimuladoAluno simuladoAluno, FinalizarSimuladoRequest request, CorrecaoDeRespostas.Resultado correcao, int totalQuestoes) {
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

}
