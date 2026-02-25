package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.model.Questao;
import studojurata_api.repository.QuestaoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestaoService {

    private final QuestaoRepository repository;

    public List<Questao> listar() { return repository.findAll(); }
    public Questao buscar(Long id) { return repository.findById(id).orElseThrow(); }
    public Questao salvar(Questao obj) { return repository.save(obj); }

    public Questao atualizar(Long id, Questao obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    public void deletar(Long id) { repository.deleteById(id); }
}