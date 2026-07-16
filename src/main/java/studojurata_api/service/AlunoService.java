package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.Aluno;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.AlunoRepository;

import java.util.List;

/** Correção 5.1 (Segunda Análise Crítica): controller passa a usar este service, não mais o Repository. */
@Service
@RequiredArgsConstructor
public class AlunoService {

    private final AlunoRepository repository;

    public List<Aluno> listar() { return repository.findAll(); }

    public Aluno buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno " + id + " não encontrado."));
    }

    public Aluno salvar(Aluno obj) { return repository.save(obj); }

    public Aluno atualizar(Long id, Aluno obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    /**
     * Soft-delete (item 4.3/5.1): Aluno não tem status próprio (é 1:1 com
     * Pessoa, ver correção 2.1) — "excluir" um aluno com histórico
     * pedagógico (notas, matrículas, simulados) marca a Pessoa vinculada
     * como INATIVA, preservando a linha física e todas as FKs históricas.
     */
    @Transactional
    public void deletar(Long id) {
        Aluno aluno = buscar(id);
        if (aluno.getPessoa() != null) {
            aluno.getPessoa().setStatus(StatusAtivoInativo.INATIVO);
        }
    }
}
