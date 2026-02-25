package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.repository.TurmaDisciplinaRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TurmaDisciplinaService {

    private final TurmaDisciplinaRepository repository;

    public List<TurmaDisciplina> listar() { return repository.findAll(); }
    public TurmaDisciplina buscar(Long id) { return repository.findById(id).orElseThrow(); }
    public TurmaDisciplina salvar(TurmaDisciplina obj) { return repository.save(obj); }

    public TurmaDisciplina atualizar(Long id, TurmaDisciplina obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    public void deletar(Long id) { repository.deleteById(id); }
}