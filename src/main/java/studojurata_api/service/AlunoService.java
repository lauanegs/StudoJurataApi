package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.model.Aluno;
import studojurata_api.repository.AlunoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlunoService {

    private final AlunoRepository repository;

    public List<Aluno> listar() { return repository.findAll(); }
    public Aluno buscar(Long id) { return repository.findById(id).orElseThrow(); }
    public Aluno salvar(Aluno obj) { return repository.save(obj); }

    public Aluno atualizar(Long id, Aluno obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    public void deletar(Long id) { repository.deleteById(id); }
}