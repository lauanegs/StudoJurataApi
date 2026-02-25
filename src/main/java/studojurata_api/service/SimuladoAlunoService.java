package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.repository.SimuladoAlunoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SimuladoAlunoService {

    private final SimuladoAlunoRepository repository;

    public List<SimuladoAluno> listar() { return repository.findAll(); }
    public SimuladoAluno buscar(Long id) { return repository.findById(id).orElseThrow(); }
    public SimuladoAluno salvar(SimuladoAluno obj) { return repository.save(obj); }

    public SimuladoAluno atualizar(Long id, SimuladoAluno obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    public void deletar(Long id) { repository.deleteById(id); }
}