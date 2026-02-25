package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.model.Disciplina;
import studojurata_api.repository.DisciplinaRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DisciplinaService {

    private final DisciplinaRepository repository;

    public List<Disciplina> listar() { return repository.findAll(); }
    public Disciplina buscar(Long id) { return repository.findById(id).orElseThrow(); }
    public Disciplina salvar(Disciplina obj) { return repository.save(obj); }

    public Disciplina atualizar(Long id, Disciplina obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    public void deletar(Long id) { repository.deleteById(id); }
}