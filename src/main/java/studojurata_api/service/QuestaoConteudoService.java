package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.model.QuestaoConteudo;
import studojurata_api.repository.QuestaoConteudoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestaoConteudoService {

    private final QuestaoConteudoRepository repository;

    public List<QuestaoConteudo> listar() { return repository.findAll(); }
    public QuestaoConteudo buscar(Long id) { return repository.findById(id).orElseThrow(); }
    public QuestaoConteudo salvar(QuestaoConteudo obj) { return repository.save(obj); }

    public QuestaoConteudo atualizar(Long id, QuestaoConteudo obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    public void deletar(Long id) { repository.deleteById(id); }
}