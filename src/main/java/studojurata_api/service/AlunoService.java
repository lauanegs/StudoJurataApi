package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.model.Aluno;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.AlunoTurmaRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlunoService {

    private final AlunoRepository repository;
    private final AlunoTurmaRepository alunoTurmaRepository;

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
     * Soft-delete pela Pessoa, já que Aluno não tem status próprio. Recusa
     * aluno com qualquer matrícula: a exclusão serve só para descartar
     * cadastro feito por engano.
     */
    @Transactional
    public void deletar(Long id) {
        Aluno aluno = buscar(id);
        if (alunoTurmaRepository.existsByAluno_Id(id)) {
            throw new RegraNegocioException(
                    "Este aluno já teve matrícula em turma e não pode ser inativado.");
        }
        if (aluno.getPessoa() != null) {
            aluno.getPessoa().setStatus(StatusAtivoInativo.INATIVO);
        }
    }

    @Transactional
    public Aluno ativar(Long id) {
        Aluno aluno = buscar(id);
        if (aluno.getPessoa() != null) {
            aluno.getPessoa().setStatus(StatusAtivoInativo.ATIVO);
        }
        return aluno;
    }
}
