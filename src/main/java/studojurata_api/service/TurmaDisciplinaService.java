package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.TurmaDisciplinaRepository;

import java.util.List;

/** Correção 5.1: controller passa a usar este service, não mais o Repository. */
@Service
@RequiredArgsConstructor
public class TurmaDisciplinaService {

    private final TurmaDisciplinaRepository repository;

    public List<TurmaDisciplina> listar() { return repository.findAll(); }

    public TurmaDisciplina buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vínculo turma-disciplina " + id + " não encontrado."));
    }

    public TurmaDisciplina salvar(TurmaDisciplina obj) {
        if (obj.getStatus() == null) obj.setStatus(StatusAtivoInativo.ATIVO);
        return repository.save(obj);
    }

    public TurmaDisciplina atualizar(Long id, TurmaDisciplina obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    /** Soft-delete (item 4.3/5.1): existem PlanoEnsino/Aula/Simulado pendurados via esta associação. */
    public void deletar(Long id) {
        TurmaDisciplina turmaDisciplina = buscar(id);
        turmaDisciplina.setStatus(StatusAtivoInativo.INATIVO);
        repository.save(turmaDisciplina);
    }
}
