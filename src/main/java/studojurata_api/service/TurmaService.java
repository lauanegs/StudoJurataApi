package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.model.Turma;
import studojurata_api.repository.TurmaRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TurmaService {

    private final TurmaRepository repository;

    public List<Turma> listar() { return repository.findAll(); }
    public Turma buscar(Long id) { return repository.findById(id).orElseThrow(); }
    public Turma salvar(Turma obj) { return repository.save(obj); }

    public Turma atualizar(Long id, Turma obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    public void deletar(Long id) { repository.deleteById(id); }
}