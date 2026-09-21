package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.Curso;
import studojurata_api.model.PlanoEnsino;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.model.enums.StatusPlano;
import studojurata_api.repository.CursoRepository;
import studojurata_api.repository.PlanoEnsinoRepository;
import studojurata_api.security.EscolaContext;
import studojurata_api.security.EscopoUsuario;
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
    private final PlanoAulaService planoAulaService;
    private final EscolaContext escolaContext;
    private final UsuarioAutenticado usuarioAutenticado;
    private final EscopoUsuario escopoUsuario;

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

    public PlanoEnsino salvar(PlanoEnsino obj) {
        validarCurso(obj);
        if (obj.getStatus() == null) obj.setStatus(StatusPlano.ATIVO);
        PlanoEnsino salvo = repository.save(obj);
        planoAulaService.gerarSeNecessario(salvo);
        return salvo;
    }

    public PlanoEnsino atualizar(Long id, PlanoEnsino obj) {
        validarCurso(obj);
        obj.setId(id);
        PlanoEnsino salvo = repository.save(obj);
        // Plano genérico que ganhou turma numa edição também passa a ter plano de aula.
        planoAulaService.gerarSeNecessario(salvo);
        return salvo;
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
        plano.setStatus(StatusPlano.CONCLUIDO);
        repository.save(plano);
    }
}
