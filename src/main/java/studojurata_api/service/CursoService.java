package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.Curso;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.CursoRepository;
import studojurata_api.security.EscolaContext;

import java.util.List;

/**
 * atualizar() ignora a escola enviada: um curso nunca muda de escola.
 * cargaHorariaTotal enviado também é ignorado, pois é sempre derivado da
 * grade (CursoDisciplinaService).
 */
@Service
@RequiredArgsConstructor
public class CursoService {

    private final CursoRepository repository;
    private final EscolaContext escolaContext;

    /** Sem escola resolvível (antes do cadastro inicial da escola), não filtra. */
    public List<Curso> listar() {
        Long escolaId = escolaContext.escolaAtualId();
        return escolaId != null ? repository.findByEscola_Id(escolaId) : repository.findAll();
    }

    public Curso buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso " + id + " não encontrado."));
    }

    /** Leitura individual: catálogo da mesma escola da listagem. */
    public Curso buscarParaLeitura(Long id) {
        Curso curso = buscar(id);
        Long escolaId = escolaContext.escolaAtualId();
        if (escolaId != null && curso.getEscola() != null && !escolaId.equals(curso.getEscola().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este curso pertence a outra escola.");
        }
        return curso;
    }

    public Curso salvar(Curso obj) {
        validarNome(obj);
        if (obj.getStatus() == null) obj.setStatus(StatusAtivoInativo.ATIVO);
        obj.setCargaHorariaTotal(0);
        return repository.save(obj);
    }

    public Curso atualizar(Long id, Curso obj) {
        validarNome(obj);
        Curso existente = buscar(id);
        obj.setId(id);
        obj.setEscola(existente.getEscola());
        obj.setCargaHorariaTotal(existente.getCargaHorariaTotal());
        if (obj.getStatus() == null) obj.setStatus(existente.getStatus());
        return repository.save(obj);
    }

    private void validarNome(Curso obj) {
        if (obj.getNome() == null || obj.getNome().isBlank()) {
            throw new RequisicaoInvalidaException("Nome do curso é obrigatório.");
        }
    }

    /** Soft-delete: turmas já vinculadas a este curso preservam a referência histórica. */
    public void deletar(Long id) {
        Curso curso = buscar(id);
        curso.setStatus(StatusAtivoInativo.INATIVO);
        repository.save(curso);
    }

    public Curso ativar(Long id) {
        Curso curso = buscar(id);
        curso.setStatus(StatusAtivoInativo.ATIVO);
        return repository.save(curso);
    }
}
