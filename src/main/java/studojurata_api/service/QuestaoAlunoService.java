package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.model.QuestaoAluno;
import studojurata_api.repository.QuestaoAlunoRepository;

import java.util.List;

/**
 * O caminho principal de escrita das respostas é SimuladoAlunoService.finalizar;
 * este service atende consultas pontuais.
 */
@Service
@RequiredArgsConstructor
public class QuestaoAlunoService {

    private final QuestaoAlunoRepository repository;

    public List<QuestaoAluno> listar() { return repository.findAll(); }

    public List<QuestaoAluno> listarPorSimuladoAluno(Long simuladoAlunoId) {
        return repository.findBySimuladoAlunoId(simuladoAlunoId);
    }
}
