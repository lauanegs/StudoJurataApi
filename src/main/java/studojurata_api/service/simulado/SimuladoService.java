package studojurata_api.service.simulado;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import studojurata_api.model.enums.TipoUsuario;
import studojurata_api.security.EscopoProfessor;
import studojurata_api.security.SimuladoAccessGuard;
import studojurata_api.security.UsuarioAutenticado;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studojurata_api.dto.simulado.LancarSimuladoRequest;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.simulado.Simulado;
import studojurata_api.model.simulado.SimuladoAluno;
import studojurata_api.model.enums.StatusSimulado;
import studojurata_api.model.enums.StatusSimuladoQuestao;
import studojurata_api.repository.simulado.SimuladoAlunoRepository;
import studojurata_api.repository.simulado.SimuladoQuestaoRepository;
import studojurata_api.repository.simulado.SimuladoRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SimuladoService {

    private final SimuladoRepository repository;
    private final SimuladoQuestaoRepository simuladoQuestaoRepository;
    private final SimuladoAlunoRepository simuladoAlunoRepository;
    private final UsuarioAutenticado usuarioAutenticado;
    private final EscopoProfessor escopoProfessor;
    private final SimuladoAccessGuard simuladoAccessGuard;
    private final PublicacaoDeSimulado publicacao;

    /**
     * Listagem escopada: ADMINISTRADOR ve tudo; PROFESSOR ve os simulados das
     * turmas em que leciona; ALUNO ve os simulados em que tem tentativa.
     * Simulados sem turma (orfaos) seguem visiveis por decisao D-C1.
     */
    public List<Simulado> listar() {
        if (usuarioAutenticado.ehAdministrador()) {
            return repository.findAll();
        }

        Set<Long> ids = simuladoIdsVisiveis();
        return ids.isEmpty() ? List.of() : repository.findAllById(ids);
    }

    /**
     * Ids dos simulados visiveis para PROFESSOR (turmas em que leciona) e ALUNO
     * (simulados em que tem tentativa).
     *
     * <p>Simulado sem turma (orfao) fica de fora: nao ha vinculo verificavel
     * para atribuir posse, entao ele so e lido pelo ADMINISTRADOR — a mesma
     * cautela que o {@code SimuladoAccessGuard} ja aplica na escrita. Os
     * simulados orfaos continuam no banco, apenas fora do alcance do professor.
     *
     * <p>Para ADMINISTRADOR devolve conjunto vazio de proposito: quem chama
     * trata o admin antes, e aqui vazio nunca significa "todos".
     */
    public Set<Long> simuladoIdsVisiveis() {
        var usuario = usuarioAutenticado.atual();

        if (usuario.getTipoUsuario() == TipoUsuario.PROFESSOR) {
            Long professorId = usuario.getProfessor() != null ? usuario.getProfessor().getId() : null;
            Set<Long> turmaIds = escopoProfessor.turmaIdsDoProfessor(professorId);

            Set<EscopoProfessor.ParTurmaDisciplina> pares = escopoProfessor.paresTurmaDisciplinaDoProfessor(professorId);

            Set<Long> ids = new LinkedHashSet<>();
            if (!turmaIds.isEmpty()) {
                repository.findByTurma_IdIn(turmaIds).stream()
                        // Turma não basta: a disciplina do simulado também precisa ser dele naquela turma.
                        .filter(simulado -> simulado.getDisciplina() != null && pares.contains(
                                new EscopoProfessor.ParTurmaDisciplina(
                                        simulado.getTurma().getId(), simulado.getDisciplina().getId())))
                        .map(Simulado::getId)
                        .filter(Objects::nonNull)
                        .forEach(ids::add);
            }
            return ids;
        }

        if (usuario.getTipoUsuario() == TipoUsuario.ALUNO) {
            Long alunoId = usuario.getAluno() != null ? usuario.getAluno().getId() : null;
            if (alunoId == null) {
                return Set.of();
            }
            return simuladoAlunoRepository.findByAlunoId(alunoId).stream()
                    .map(SimuladoAluno::getSimulado)
                    .filter(Objects::nonNull)
                    .map(Simulado::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        return Set.of();
    }

    public Simulado buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Simulado " + id + " não encontrado."));
    }

    /**
     * Leitura individual (GET /simulados/{id}) com a <b>mesma visibilidade da
     * listagem</b>: ADMIN vê tudo (órfãos inclusive), PROFESSOR os simulados das
     * suas turmas e ALUNO os simulados em que tem tentativa.
     *
     * <p>Sem isso, qualquer usuário autenticado leria por id o simulado de outra
     * turma (título, datas, disciplina e status). O uso interno de escrita
     * continua em {@link #buscar}, porque lá quem decide é o
     * {@code SimuladoAccessGuard}, com as regras completas de escopo.
     */
    public Simulado buscarParaLeitura(Long id) {
        Simulado simulado = buscar(id);

        if (usuarioAutenticado.ehAdministrador()) {
            return simulado;
        }
        if (!simuladoIdsVisiveis().contains(simulado.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você só pode acessar simulados das suas turmas.");
        }

        return simulado;
    }

    /**
     * Criação de simulado (POST /simulados).
     *
     * <p>O objeto chega do mapper com turma, disciplina e plano já resolvidos por
     * id no banco; o guard decide se o professor pode escrever nesse escopo. Para
     * o professor não existe simulado novo sem turma ou sem disciplina — o caso
     * fica com o administrador.
     */
    public Simulado salvar(Simulado obj) {
        simuladoAccessGuard.garantirEscrita(obj);
        simuladoAccessGuard.garantirPlanoCompativel(obj);
        simuladoAccessGuard.garantirDisciplinaAtiva(obj);
        validarCamposDoSimulado(obj);
        // Simulado novo nasce sem vínculo de questão: a quantidade é a real.
        obj.setQuantidadeQuestoes(0);
        if (obj.getStatus() == null) {
            obj.setStatus(StatusSimulado.RASCUNHO);
        }
        return repository.save(obj);
    }

    /**
     * Só em RASCUNHO: depois de publicado, os alunos já foram convocados com
     * base nos dados do lançamento.
     *
     * <p>O escopo é validado para o simulado atual e para os valores enviados:
     * sem isso, um professor editaria rascunho alheio ou moveria o próprio
     * simulado para uma turma/disciplina que não leciona. A checagem de escopo
     * vem antes da de status para não revelar o estado de simulado alheio.
     */
    public Simulado atualizar(Long id, Simulado obj) {
        simuladoAccessGuard.garantirPerfilDeEscrita();
        Simulado existente = buscar(id);
        simuladoAccessGuard.garantirEscrita(existente, obj);

        if (existente.getStatus() != StatusSimulado.RASCUNHO) {
            throw new RegraNegocioException(
                    "Só é possível editar os dados de um simulado enquanto ele está em RASCUNHO.");
        }
        // Depois da regra de status: simulado publicado continua recusado com a
        // mensagem de sempre, mesmo que o corpo venha sem plano.
        simuladoAccessGuard.garantirPlanoCompativel(obj);
        simuladoAccessGuard.garantirDisciplinaAtiva(obj);
        validarCamposDoSimulado(obj);
        obj.setId(id);
        // O status só muda por lancar()/encerrar().
        obj.setStatus(existente.getStatus());
        // Mesma regra da criação: a quantidade é a contagem real, não a do corpo.
        obj.setQuantidadeQuestoes(contarQuestoesAtivas(id));
        return repository.save(obj);
    }

    /**
     * Mantém {@code Simulado.quantidadeQuestoes} igual à quantidade real de
     * vínculos ATIVA deste simulado.
     *
     * <p>É contagem no banco, não incremento local: inclusão repetida, remoção
     * de vínculo inexistente ou operações concorrentes não fazem o número
     * divergir dos relacionamentos. Vínculos REMOVIDA não entram na conta, e o
     * valor nunca fica negativo (a contagem começa em zero).
     */
    @Transactional
    public Simulado sincronizarQuantidadeDeQuestoes(Simulado simulado) {
        if (simulado == null || simulado.getId() == null) {
            return simulado;
        }

        int ativas = contarQuestoesAtivas(simulado.getId());
        if (Objects.equals(simulado.getQuantidadeQuestoes(), ativas)) {
            return simulado;
        }

        simulado.setQuantidadeQuestoes(ativas);
        return repository.save(simulado);
    }

    private int contarQuestoesAtivas(Long simuladoId) {
        return (int) simuladoQuestaoRepository.countBySimuladoIdAndStatus(simuladoId, StatusSimuladoQuestao.ATIVA);
    }

    /** Único campo editável depois do lançamento. */
    @Transactional
    public Simulado estenderDisponibilidade(Long id, LocalDateTime novaDataFim) {
        simuladoAccessGuard.garantirPerfilDeEscrita();
        Simulado simulado = buscar(id);
        simuladoAccessGuard.garantirEscrita(simulado);

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

        simuladoAccessGuard.garantirPerfilDeEscrita();
        Simulado simulado = buscar(simuladoId);
        simuladoAccessGuard.garantirEscrita(simulado);

        return publicacao.publicar(simulado, alunoIdsEspecificos);
    }

    /**
     * Publicação na data de início, feita pelo job agendado (sem sessão): o
     * simulado já passou pelo escopo do professor quando foi criado. Não lança
     * quando faltam condições (sem questões, questões pendentes, destinatários
     * indefinidos) — nesse caso a regra de negócio explica o motivo.
     */
    @Transactional
    public Simulado lancarAutomaticamente(Long simuladoId) {
        return publicacao.publicar(buscar(simuladoId), null);
    }

    /**
     * Janela de aplicação, duração e nota máxima do simulado. O front já valida
     * parte disso; aqui é o contrato — quem chama a API direto passa pelas
     * mesmas regras. Datas são {@code LocalDateTime} (sem fuso) e a comparação é
     * feita direto entre elas, sem conversão.
     */
    private void validarCamposDoSimulado(Simulado obj) {
        if (obj.getDataInicio() == null) {
            throw new RequisicaoInvalidaException("Data de início é obrigatória.");
        }
        if (obj.getDataFim() != null && !obj.getDataFim().isAfter(obj.getDataInicio())) {
            throw new RequisicaoInvalidaException("A data final deve ser posterior à data de início.");
        }
        if (obj.getTempoLimite() != null && obj.getTempoLimite() <= 0) {
            throw new RequisicaoInvalidaException("O tempo limite deve ser maior que zero.");
        }
        if (obj.getNotaMaxima() != null && obj.getNotaMaxima() <= 0) {
            throw new RequisicaoInvalidaException("A nota máxima deve ser maior que zero.");
        }
    }

    @Transactional
    public Simulado encerrar(Long simuladoId) {
        simuladoAccessGuard.garantirPerfilDeEscrita();
        Simulado simulado = buscar(simuladoId);
        simuladoAccessGuard.garantirEscrita(simulado);

        return publicacao.encerrar(simulado);
    }

    /** Encerramento na data final, feito pelo job agendado (sem sessão). */
    @Transactional
    public Simulado encerrarAutomaticamente(Long simuladoId) {
        return publicacao.encerrar(buscar(simuladoId));
    }
}
