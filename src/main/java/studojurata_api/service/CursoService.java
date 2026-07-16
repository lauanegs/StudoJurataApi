package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.Curso;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.CursoRepository;
import studojurata_api.security.EscolaContext;

import java.util.List;

/**
 * Curso: entidade própria (ver Curso.java) — cada Turma passa a apontar para
 * um Curso cadastrado, em vez de repetir o nome do curso como texto livre.
 *
 * Correção 3.2 da Quarta Análise Crítica: nome passa a ser validado de
 * forma amigável (400), pelo mesmo padrão já usado em Turma.curso e
 * PlanoEnsino.periodoLetivo — antes, salvar sem nome estourava direto a
 * constraint do banco como erro 500 genérico.
 * Correção 3.4: atualizar() não aceita mais trocar a escola de um Curso já
 * existente — a escola enviada no corpo da requisição é ignorada,
 * preservando sempre a escola original do curso.
 */
@Service
@RequiredArgsConstructor
public class CursoService {

    private final CursoRepository repository;
    private final EscolaContext escolaContext;

    /** Filtra pela escola do usuário autenticado; se não houver escola resolvível, devolve tudo (bootstrapping). */
    public List<Curso> listar() {
        Long escolaId = escolaContext.escolaAtualId();
        return escolaId != null ? repository.findByEscola_Id(escolaId) : repository.findAll();
    }

    public Curso buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso " + id + " não encontrado."));
    }

    public Curso salvar(Curso obj) {
        validarNome(obj);
        if (obj.getStatus() == null) obj.setStatus(StatusAtivoInativo.ATIVO);
        return repository.save(obj);
    }

    public Curso atualizar(Long id, Curso obj) {
        validarNome(obj);
        Curso existente = buscar(id);
        obj.setId(id);
        obj.setEscola(existente.getEscola());
        if (obj.getStatus() == null) obj.setStatus(existente.getStatus());
        return repository.save(obj);
    }

    private void validarNome(Curso obj) {
        if (obj.getNome() == null || obj.getNome().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome do curso é obrigatório.");
        }
    }

    /** Soft-delete: turmas já vinculadas a este curso preservam a referência histórica. */
    public void deletar(Long id) {
        Curso curso = buscar(id);
        curso.setStatus(StatusAtivoInativo.INATIVO);
        repository.save(curso);
    }
}
