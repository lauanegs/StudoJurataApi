package studojurata_api.service;

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
import studojurata_api.dto.LancarSimuladoRequest;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.ia.model.SimuladoGeradoIA;
import studojurata_api.ia.repository.SimuladoGeradoIARepository;
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
    private final SimuladoGeradoIARepository simuladoGeradoIARepository;
    private final AlunoTurmaService alunoTurmaService;
    private final AlunoRepository alunoRepository;
    private final UsuarioAutenticado usuarioAutenticado;
    private final EscopoProfessor escopoProfessor;
    private final SimuladoAccessGuard simuladoAccessGuard;

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

            Set<Long> ids = new LinkedHashSet<>();
            if (!turmaIds.isEmpty()) {
                repository.findByTurma_IdIn(turmaIds).stream()
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
