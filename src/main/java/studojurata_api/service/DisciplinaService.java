package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.Disciplina;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.DisciplinaRepository;
import studojurata_api.security.EscolaContext;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DisciplinaService {

    private final DisciplinaRepository repository;
    private final EscolaContext escolaContext;

    /** Sem escola resolvível (antes do cadastro inicial da escola), não filtra. */
    public List<Disciplina> listar() {
        Long escolaId = escolaContext.escolaAtualId();
        return escolaId != null ? repository.findByEscola_Id(escolaId) : repository.findAll();
    }

    public Disciplina buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Disciplina " + id + " não encontrada."));
    }

    /** Leitura individual: catálogo da mesma escola da listagem (disciplina não é dado pessoal). */
    public Disciplina buscarParaLeitura(Long id) {
        Disciplina disciplina = buscar(id);
        Long escolaId = escolaContext.escolaAtualId();
        if (escolaId != null && disciplina.getEscola() != null && !escolaId.equals(disciplina.getEscola().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta disciplina pertence a outra escola.");
        }
        return disciplina;
    }

    public Disciplina salvar(Disciplina obj) {
        if (obj.getStatus() == null) obj.setStatus(StatusAtivoInativo.ATIVO);
        return repository.save(obj);
    }

    public Disciplina atualizar(Long id, Disciplina obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    /** Soft-delete: disciplina pode ter questões, notas e planos de ensino vinculados. */
    public void deletar(Long id) {
        Disciplina disciplina = buscar(id);
        disciplina.setStatus(StatusAtivoInativo.INATIVO);
        repository.save(disciplina);
    }

    public Disciplina ativar(Long id) {
        Disciplina disciplina = buscar(id);
        disciplina.setStatus(StatusAtivoInativo.ATIVO);
        return repository.save(disciplina);
    }
}
