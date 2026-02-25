package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.model.QuestaoAluno;
import studojurata_api.repository.QuestaoAlunoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestaoAlunoService {

    private final QuestaoAlunoRepository repository;

    public List<QuestaoAluno> listar() { return repository.findAll(); }
    public QuestaoAluno buscar(Long id) { return repository.findById(id).orElseThrow(); }
    public QuestaoAluno salvar(QuestaoAluno obj) { return repository.save(obj); }

    public QuestaoAluno atualizar(Long id, QuestaoAluno obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    public void deletar(Long id) { repository.deleteById(id); }
}