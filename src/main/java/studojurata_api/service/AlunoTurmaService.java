package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.model.AlunoTurma;
import studojurata_api.repository.AlunoTurmaRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlunoTurmaService {

    private final AlunoTurmaRepository repository;

    public List<AlunoTurma> listar() { return repository.findAll(); }
    public AlunoTurma buscar(Long id) { return repository.findById(id).orElseThrow(); }
    public AlunoTurma salvar(AlunoTurma obj) { return repository.save(obj); }

    public AlunoTurma atualizar(Long id, AlunoTurma obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    public void deletar(Long id) { repository.deleteById(id); }
}