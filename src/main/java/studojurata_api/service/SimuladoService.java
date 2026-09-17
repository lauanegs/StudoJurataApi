package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.dto.LancarSimuladoRequest;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.Aluno;
import studojurata_api.model.AlunoTurma;
import studojurata_api.model.Simulado;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.model.SimuladoQuestao;
import studojurata_api.model.enums.StatusQuestao;
import studojurata_api.model.enums.StatusSimulado;
import studojurata_api.model.enums.StatusSimuladoAluno;
import studojurata_api.model.enums.StatusSimuladoQuestao;
import studojurata_api.model.enums.TipoDestinacaoSimulado;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.SimuladoAlunoRepository;
import studojurata_api.repository.SimuladoQuestaoRepository;
import studojurata_api.repository.SimuladoRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SimuladoService {

    private final SimuladoRepository repository;
    private final SimuladoQuestaoRepository simuladoQuestaoRepository;
    private final SimuladoAlunoRepository simuladoAlunoRepository;
    private final AlunoTurmaService alunoTurmaService;
    private final AlunoRepository alunoRepository;

    public List<Simulado> listar() { return repository.findAll(); }

    public Simulado buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Simulado " + id + " não encontrado."));
    }

    public Simulado salvar(Simulado obj) {
        if (obj.getStatus() == null) {
            obj.setStatus(StatusSimulado.RASCUNHO);
        }
        return repository.save(obj);
    }

    /**
     * Só em RASCUNHO: depois de publicado, os alunos já foram convocados com
     * base nos dados do lançamento.
     */
    public Simulado atualizar(Long id, Simulado obj) {
        Simulado existente = buscar(id);
        if (existente.getStatus() != StatusSimulado.RASCUNHO) {
            throw new RegraNegocioException(
                    "Só é possível editar os dados de um simulado enquanto ele está em RASCUNHO.");
        }
        obj.setId(id);
        // O status só muda por lancar()/encerrar().
        obj.setStatus(existente.getStatus());
        return repository.save(obj);
    }

    /** Único campo editável depois do lançamento. */
    @Transactional
    public Simulado estenderDisponibilidade(Long id, LocalDateTime novaDataFim) {
        Simulado simulado = buscar(id);
        if (simulado.getStatus() != StatusSimulado.PUBLICADO) {
            throw new RegraNegocioException(
                    "Só é possível estender a disponibilidade de um simulado PUBLICADO.");
        }
        if (novaDataFim != null && simulado.getDataInicio() != null && !novaDataFim.isAfter(simulado.getDataInicio())) {
            throw new RequisicaoInvalidaException("A nova data final deve ser posterior à data de início.");
        }
        simulado.setDataFim(novaDataFim);
        return repository.save(simulado);
    }

    /**
     * Exige questões ativas e todas aprovadas. Cria uma tentativa PENDENTE
     * por aluno elegível: matrículas ATIVAS da turma (TODOS) ou os alunos
     * informados (ESPECIFICO).
     */
    @Transactional
    public Simulado lancar(Long simuladoId, LancarSimuladoRequest request) {
        List<Long> alunoIdsEspecificos = request != null ? request.getAlunoIds() : null;
        Simulado simulado = buscar(simuladoId);

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
            if (alunoIdsEspecificos == null || alunoIdsEspecificos.isEmpty()) {
                throw new RequisicaoInvalidaException(
                        "Informe ao menos um aluno para um simulado com destinação ESPECIFICO.");
            }
            return alunoIdsEspecificos.stream()
                    .map(id -> alunoRepository.findById(id)
                            .orElseThrow(() -> new RecursoNaoEncontradoException(
                                    "Aluno " + id + " não encontrado.")))
                    .toList();
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

    @Transactional
    public Simulado encerrar(Long simuladoId) {
        Simulado simulado = buscar(simuladoId);
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
