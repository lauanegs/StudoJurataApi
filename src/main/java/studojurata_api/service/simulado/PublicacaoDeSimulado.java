package studojurata_api.service.simulado;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.ia.model.SimuladoGeradoIA;
import studojurata_api.ia.repository.SimuladoGeradoIARepository;
import studojurata_api.model.pessoa.Aluno;
import studojurata_api.model.turma.AlunoTurma;
import studojurata_api.model.simulado.Simulado;
import studojurata_api.model.simulado.SimuladoAluno;
import studojurata_api.model.simulado.SimuladoQuestao;
import studojurata_api.model.enums.StatusQuestao;
import studojurata_api.model.enums.StatusSimulado;
import studojurata_api.model.enums.StatusSimuladoAluno;
import studojurata_api.model.enums.StatusSimuladoQuestao;
import studojurata_api.model.enums.TipoDestinacaoSimulado;
import studojurata_api.repository.pessoa.AlunoRepository;
import studojurata_api.repository.simulado.SimuladoAlunoRepository;
import studojurata_api.repository.simulado.SimuladoQuestaoRepository;
import studojurata_api.repository.simulado.SimuladoRepository;
import studojurata_api.security.UsuarioAutenticado;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import studojurata_api.service.turma.AlunoTurmaService;

/**
 * Transições de status do simulado: publicação (cria as tentativas dos alunos
 * elegíveis) e encerramento. Quem chama decide a autorização — o professor
 * passa pelo {@link SimuladoService}, e os jobs agendados rodam sem sessão.
 */
@Component
@RequiredArgsConstructor
public class PublicacaoDeSimulado {

    private final SimuladoRepository repository;
    private final SimuladoQuestaoRepository simuladoQuestaoRepository;
    private final SimuladoAlunoRepository simuladoAlunoRepository;
    private final SimuladoGeradoIARepository simuladoGeradoIARepository;
    private final AlunoTurmaService alunoTurmaService;
    private final AlunoRepository alunoRepository;
    private final UsuarioAutenticado usuarioAutenticado;

    public Simulado publicar(Simulado simulado, List<Long> alunoIdsEspecificos) {
        Long simuladoId = simulado.getId();

        if (simulado.getStatus() != StatusSimulado.RASCUNHO) {
            throw new RegraNegocioException("Este simulado já foi lançado ou está encerrado.");
        }

        List<SimuladoQuestao> questoesAtivas = simuladoQuestaoRepository
                .findBySimuladoIdAndStatusOrderByOrdem(simuladoId, StatusSimuladoQuestao.ATIVA);
        if (questoesAtivas.isEmpty()) {
            throw new RegraNegocioException("Não é possível lançar um simulado sem questões.");
        }

        List<Long> naoAprovadas = questoesAtivas.stream()
                .map(SimuladoQuestao::getQuestao)
                .filter(q -> q.getStatus() != StatusQuestao.APROVADA)
                .map(q -> q.getId())
                .toList();
        if (!naoAprovadas.isEmpty()) {
            throw new RegraNegocioException(
                    "Existem questões pendentes de revisão neste simulado (ids: " + naoAprovadas + "). "
                            + "Todas as questões precisam estar APROVADAS antes do lançamento.");
        }

        List<Aluno> elegiveis = resolverElegiveis(simulado, alunoIdsEspecificos);

        for (Aluno aluno : elegiveis) {
            if (simuladoAlunoRepository.existsBySimuladoIdAndAlunoId(simuladoId, aluno.getId())) {
                continue; // já convocado — lançamento é idempotente
            }
            SimuladoAluno simuladoAluno = new SimuladoAluno();
            simuladoAluno.setSimulado(simulado);
            simuladoAluno.setAluno(aluno);
            simuladoAluno.setStatus(StatusSimuladoAluno.PENDENTE);
            simuladoAlunoRepository.save(simuladoAluno);
        }

        simulado.setStatus(StatusSimulado.PUBLICADO);
        if (simulado.getDataInicio() == null) {
            simulado.setDataInicio(LocalDateTime.now());
        }
        return repository.save(simulado);
    }

    private List<Aluno> resolverElegiveis(Simulado simulado, List<Long> alunoIdsEspecificos) {
        if (simulado.getTipoDestinacao() == TipoDestinacaoSimulado.ESPECIFICO) {
            List<Aluno> elegiveisDoVinculoIA = elegiveisDoVinculoIA(simulado, alunoIdsEspecificos);
            if (elegiveisDoVinculoIA != null) {
                return elegiveisDoVinculoIA;
            }

            if (alunoIdsEspecificos == null || alunoIdsEspecificos.isEmpty()) {
                throw new RequisicaoInvalidaException(
                        "Informe ao menos um aluno para um simulado com destinação ESPECIFICO.");
            }
            List<Aluno> alunos = alunoIdsEspecificos.stream()
                    .map(id -> alunoRepository.findById(id)
                            .orElseThrow(() -> new RecursoNaoEncontradoException(
                                    "Aluno " + id + " não encontrado.")))
                    .toList();
            validarAlunosMatriculadosNaTurma(simulado, alunos);
            return alunos;
        }

        if (simulado.getTurma() == null) {
            throw new RequisicaoInvalidaException(
                    "Simulado com destinação TODOS precisa estar vinculado a uma turma.");
        }
        return alunoTurmaService.ativosPorTurma(simulado.getTurma().getId())
                .stream()
                .map(AlunoTurma::getAluno)
                .toList();
    }

    /**
     * Destinatário de simulado gerado pela IA: quem manda é o vínculo
     * aluno × conteúdo gravado na geração, não o corpo da requisição — o
     * professor aprova as questões e o simulado vai para o aluno que a IA
     * indicou. Mandar outro aluno é recusado.
     *
     * @return alunos do vínculo, ou {@code null} quando o simulado não veio da
     *         IA (aí vale a regra normal de destinação específica).
     */
    private List<Aluno> elegiveisDoVinculoIA(Simulado simulado, List<Long> alunoIdsInformados) {
        if (simulado.getId() == null) {
            return null;
        }

        Set<Long> alunosDoVinculo = simuladoGeradoIARepository.findBySimulado_Id(simulado.getId()).stream()
                .map(SimuladoGeradoIA::getAluno)
                .filter(Objects::nonNull)
                .map(Aluno::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (alunosDoVinculo.isEmpty()) {
            return null;
        }
        if (alunoIdsInformados != null && !alunosDoVinculo.containsAll(alunoIdsInformados)) {
            throw new RequisicaoInvalidaException(
                    "Este simulado foi gerado pela IA para outro(s) aluno(s): o destinatário não pode ser alterado.");
        }

        return alunosDoVinculo.stream()
                .map(alunoId -> alunoRepository.findById(alunoId)
                        .orElseThrow(() -> new RecursoNaoEncontradoException(
                                "Aluno " + alunoId + " não encontrado.")))
                .toList();
    }

    /**
     * Simulado específico de professor só convoca aluno matriculado na turma do
     * simulado (qualquer status de matrícula: o histórico também vale, como no
     * resto do domínio). O acesso do professor à turma e à disciplina já foi
     * decidido pelo {@code SimuladoAccessGuard} antes daqui, então o que falta
     * validar é o vínculo do aluno com a turma — uma consulta só, sem N+1.
     *
     * <p>Roda antes de criar qualquer tentativa: lista com aluno inválido não
     * gera efeito parcial. O ADMIN mantém a liberdade do fluxo administrativo.
     */
    private void validarAlunosMatriculadosNaTurma(Simulado simulado, List<Aluno> alunos) {
        if (alunos.isEmpty() || usuarioAutenticado.ehAdministrador()) {
            return;
        }

        Long turmaId = simulado.getTurma() != null ? simulado.getTurma().getId() : null;
        if (turmaId == null) {
            throw new RegraNegocioException(
                    "Simulado com destinação ESPECIFICO precisa estar vinculado a uma turma.");
        }

        Set<Long> matriculados = alunoTurmaService.historicoPorTurma(turmaId).stream()
                .map(AlunoTurma::getAluno)
                .filter(Objects::nonNull)
                .map(Aluno::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Long> foraDaTurma = alunos.stream()
                .map(Aluno::getId)
                .filter(alunoId -> alunoId == null || !matriculados.contains(alunoId))
                .toList();
        if (!foraDaTurma.isEmpty()) {
            throw new RegraNegocioException(
                    "Estes alunos não têm matrícula na turma deste simulado: " + foraDaTurma + ".");
        }
    }

    public Simulado encerrar(Simulado simulado) {
        if (simulado.getStatus() != StatusSimulado.PUBLICADO) {
            throw new RegraNegocioException("Só é possível encerrar um simulado que esteja PUBLICADO.");
        }
        simulado.setStatus(StatusSimulado.ENCERRADO);
        if (simulado.getDataFim() == null) {
            simulado.setDataFim(LocalDateTime.now());
        }
        return repository.save(simulado);
    }
}
