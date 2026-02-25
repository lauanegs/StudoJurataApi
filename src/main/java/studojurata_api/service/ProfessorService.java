package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.model.Professor;
import studojurata_api.repository.ProfessorRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfessorService {

    private final ProfessorRepository repository;

    public List<Professor> listar() { return repository.findAll(); }
    public Professor buscar(Long id) { return repository.findById(id).orElseThrow(); }
    public Professor salvar(Professor obj) { return repository.save(obj); }

    public Professor atualizar(Long id, Professor obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    public void deletar(Long id) { repository.deleteById(id); }
}