package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.Curso;
import studojurata_api.model.CursoDisciplina;
import studojurata_api.model.Disciplina;
import studojurata_api.model.PlanoEnsino;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.model.enums.StatusPlano;
import studojurata_api.repository.CursoRepository;
import studojurata_api.repository.CursoDisciplinaRepository;
import studojurata_api.repository.PlanoEnsinoRepository;
import studojurata_api.repository.TurmaDisciplinaRepository;
import studojurata_api.security.EscolaContext;
import studojurata_api.security.EscopoUsuario;
import studojurata_api.security.PlanejamentoAccessGuard;
import studojurata_api.security.UsuarioAutenticado;
import studojurata_api.model.enums.TipoUsuario;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * validarCurso recusa curso de outra escola (403) ou inativo (409), como em
 * TurmaService.
 */
@Service
@RequiredArgsConstructor
public class PlanoEnsinoService {

    private final PlanoEnsinoRepository repository;
    private final CursoRepository cursoRepository;
    private final TurmaDisciplinaRepository turmaDisciplinaRepository;
    private final CursoDisciplinaRepository cursoDisciplinaRepository;
    private final PlanoAulaService planoAulaService;
    private final EscolaContext escolaContext;
    private final UsuarioAutenticado usuarioAutenticado;
    private final EscopoUsuario escopoUsuario;
    private final PlanejamentoAccessGuard planejamentoAccessGuard;

    /** Filtra pela escola do curso; sem escola resolvível (antes do cadastro inicial), não filtra. */
    public List<PlanoEnsino> listar() {
        var usuario = usuarioAutenticado.atual();

        if (usuario.getTipoUsuario() == TipoUsuario.ADMINISTRADOR) {
            Long escolaId = escolaContext.escolaAtualId();
            return escolaId != null ? repository.findByCurso_Escola_Id(escolaId) : repository.findAll();
        }

        Set<Long> vinculoIds = escopoUsuario.turmaDisciplinaIds();

        List<PlanoEnsino> visiveis = new ArrayList<>();
        if (!vinculoIds.isEmpty()) {
            visiveis.addAll(repository.findByTurmaDisciplina_IdIn(vinculoIds));
        }
        visiveis.addAll(repository.findByTurmaDisciplinaIsNull());
        return visiveis;
    }

    public PlanoEnsino buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plano de ensino " + id + " não encontrado."));
    }

    /** Leitura individual com a mesma visibilidade da listagem (genéricos por D-C2). */
    public PlanoEnsino buscarParaLeitura(Long id) {
        PlanoEnsino plano = buscar(id);
        planejamentoAccessGuard.garantirLeitura(vinculoId(plano),
                "Você só pode acessar planos de ensino das suas turmas.");
        return plano;
    }

    public PlanoEnsino salvar(PlanoEnsino obj) {
        planejamentoAccessGuard.garantirEscrita(vinculoId(obj),
                "Você só pode criar planos de ensino das suas turmas.");
        validarCurso(obj);
        validarTurmaDisciplina(obj, null);
        if (obj.getStatus() == null) obj.setStatus(StatusPlano.ATIVO);
        PlanoEnsino salvo = repository.save(obj);
        planoAulaService.gerarSeNecessario(salvo);
        return salvo;
    }

    public PlanoEnsino atualizar(Long id, PlanoEnsino obj) {
        // Escopo das duas pontas: o plano atual e o vínculo enviado.
        planejamentoAccessGuard.garantirEscrita(vinculoId(buscar(id)),
                "Você só pode alterar planos de ensino das suas turmas.");
        planejamentoAccessGuard.garantirEscrita(vinculoId(obj),
                "Você só pode mover planos de ensino para as suas turmas.");
        validarCurso(obj);
        validarTurmaDisciplina(obj, buscar(id));
        obj.setId(id);
        PlanoEnsino salvo = repository.save(obj);
        // Plano genérico que ganhou turma numa edição também passa a ter plano de aula.
        planoAulaService.gerarSeNecessario(salvo);
        return salvo;
    }

    /**
     * O vínculo turma+disciplina do plano precisa estar utilizável: vínculo
     * ativo, disciplina ativa e disciplina ainda na grade ativa do curso. É aqui
     * que o back fecha o caminho para o vínculo indevido — a tela já esconde as
     * opções inativadas, mas quem manda o id direto não passaria por ela.
     *
     * <p>A checagem vale só quando o vínculo é definido ou trocado: plano que
     * mantém o vínculo atual continua editável, para o histórico de quem já
     * usava a disciplina antes de ela ser inativada não ficar travado.
     */
    private void validarTurmaDisciplina(PlanoEnsino obj, PlanoEnsino existente) {
        Long vinculoId = obj.getTurmaDisciplina() != null ? obj.getTurmaDisciplina().getId() : null;
        if (vinculoId == null) return; // plano genérico segue como sempre foi

        Long vinculoAtualId = existente != null && existente.getTurmaDisciplina() != null
                ? existente.getTurmaDisciplina().getId()
                : null;
        if (existente != null && vinculoId.equals(vinculoAtualId)) return;

        TurmaDisciplina vinculo = turmaDisciplinaRepository.findById(vinculoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Vínculo turma-disciplina " + vinculoId + " não encontrado."));

        if (vinculo.getStatus() == StatusAtivoInativo.INATIVO) {
            throw new RegraNegocioException(
                    "Esta disciplina não está mais vinculada à turma e não pode receber novos planos de ensino.");
        }

        Disciplina disciplina = vinculo.getDisciplina();
        if (disciplina == null || disciplina.getStatus() == StatusAtivoInativo.INATIVO) {
            throw new RegraNegocioException(
                    "A disciplina desta turma está inativada e não pode receber novos planos de ensino.");
        }

        Long cursoId = obj.getCurso() != null ? obj.getCurso().getId() : null;
        boolean naGradeAtiva = cursoId != null && cursoDisciplinaRepository.findByCurso_Id(cursoId).stream()
                .filter(item -> item.getStatus() == StatusAtivoInativo.ATIVO)
                .map(CursoDisciplina::getDisciplina)
                .anyMatch(item -> item != null && disciplina.getId() != null && disciplina.getId().equals(item.getId()));
        if (!naGradeAtiva) {
            throw new RegraNegocioException(
                    "A disciplina desta turma não está mais na grade curricular ativa do curso.");
        }

        obj.setTurmaDisciplina(vinculo);
    }

    private void validarCurso(PlanoEnsino obj) {
        if (obj.getCurso() == null || obj.getCurso().getId() == null) {
            throw new RequisicaoInvalidaException("Curso é obrigatório: todo plano de ensino pertence a um curso.");
        }
        Curso curso = cursoRepository.findById(obj.getCurso().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso " + obj.getCurso().getId() + " não encontrado."));

        if (curso.getStatus() == StatusAtivoInativo.INATIVO) {
            throw new RegraNegocioException(
                    "O curso \"" + curso.getNome() + "\" está inativo e não pode receber novos planos de ensino.");
        }

        Long escolaId = escolaContext.escolaAtualId();
        if (escolaId != null && curso.getEscola() != null && !escolaId.equals(curso.getEscola().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Este curso pertence a outra escola e não pode ser vinculado a este plano de ensino.");
        }

        obj.setCurso(curso);
    }

    /** Soft delete: vira CONCLUIDO, preservando conteúdos, planos de aula e simulados. */
    public void deletar(Long id) {
        PlanoEnsino plano = buscar(id);
        planejamentoAccessGuard.garantirEscrita(vinculoId(plano),
                "Você só pode encerrar planos de ensino das suas turmas.");
        plano.setStatus(StatusPlano.CONCLUIDO);
        repository.save(plano);
    }

    private static Long vinculoId(PlanoEnsino plano) {
        return plano != null && plano.getTurmaDisciplina() != null ? plano.getTurmaDisciplina().getId() : null;
    }
}
