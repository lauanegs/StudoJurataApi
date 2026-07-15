package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.Aluno;
import studojurata_api.model.AlunoTurma;
import studojurata_api.model.Simulado;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.model.SimuladoQuestao;
import studojurata_api.model.enums.StatusMatricula;
import studojurata_api.model.enums.StatusQuestao;
import studojurata_api.model.enums.StatusSimulado;
import studojurata_api.model.enums.StatusSimuladoAluno;
import studojurata_api.model.enums.StatusSimuladoQuestao;
import studojurata_api.model.enums.TipoDestinacaoSimulado;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.AlunoTurmaRepository;
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
    private final AlunoTurmaRepository alunoTurmaRepository;
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
     * Edição dos dados do simulado só é permitida enquanto ele está em
     * RASCUNHO — após publicado, alunos elegíveis já foram convocados
     * (SimuladoAluno) com base nos dados vigentes no momento do lançamento.
     */
    public Simulado atualizar(Long id, Simulado obj) {
        Simulado existente = buscar(id);
        if (existente.getStatus() != StatusSimulado.RASCUNHO) {
            throw new RegraNegocioException(
                    "Só é possível editar os dados de um simulado enquanto ele está em RASCUNHO.");
        }
        obj.setId(id);
        // preserva o status (RASCUNHO) — o DTO de entrada não expõe este campo,
        // que é controlado exclusivamente pelo fluxo de lancar()/encerrar().
        obj.setStatus(existente.getStatus());
        return repository.save(obj);
    }

    public void deletar(Long id) { repository.deleteById(id); }

    /**
     * Lança o simulado (item 1.3 da Análise Crítica).
     *
     * Valida que existem questões ativas vinculadas e que todas já foram
     * aprovadas (item 7.3 — não é possível lançar um simulado com questões
     * ainda pendentes de revisão do professor). Em seguida transiciona o
     * status para PUBLICADO e cria um SimuladoAluno (status PENDENTE) para
     * cada aluno elegível: todos os alunos com matrícula ATIVA na turma
     * (tipoDestinacao = TODOS) ou apenas os alunos informados
     * (tipoDestinacao = ESPECIFICO).
     */
    @Transactional
    public Simulado lancar(Long simuladoId, List<Long> alunoIdsEspecificos) {
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
        return alunoTurmaRepository
                .findByTurmaIdAndStatus(simulado.getTurma().getId(), StatusMatricula.ATIVA)
                .stream()
                .map(AlunoTurma::getAluno)
                .toList();
    }

    /**
     * Encerra manualmente o simulado (ex.: janela dataFim atingida): deixa
     * de aceitar novas tentativas/finalizações.
     */
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
