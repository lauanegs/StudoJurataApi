package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.model.SimuladoQuestao;
import studojurata_api.repository.SimuladoQuestaoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SimuladoQuestaoService {

    private final SimuladoQuestaoRepository repository;

    public List<SimuladoQuestao> listar() { return repository.findAll(); }
    public SimuladoQuestao buscar(Long id) { return repository.findById(id).orElseThrow(); }
    public SimuladoQuestao salvar(SimuladoQuestao obj) { return repository.save(obj); }

    public SimuladoQuestao atualizar(Long id, SimuladoQuestao obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    public void deletar(Long id) { repository.deleteById(id); }
}