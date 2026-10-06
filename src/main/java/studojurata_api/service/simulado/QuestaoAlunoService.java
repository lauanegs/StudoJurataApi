package studojurata_api.service.simulado;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.model.simulado.QuestaoAluno;
import studojurata_api.model.simulado.SimuladoAluno;
import studojurata_api.repository.simulado.QuestaoAlunoRepository;
import studojurata_api.repository.simulado.SimuladoAlunoRepository;
import studojurata_api.security.UsuarioAutenticado;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** O caminho principal de escrita das respostas é SimuladoAlunoService.finalizar */
@Service
@RequiredArgsConstructor
public class QuestaoAlunoService {
    private final QuestaoAlunoRepository repository;
    private final SimuladoAlunoRepository simuladoAlunoRepository;
    private final SimuladoService simuladoService;
    private final UsuarioAutenticado usuarioAutenticado;

    /** Listagem escopada: ADMINISTRADOR ve todas as respostas */
    public List<QuestaoAluno> listar() {
        if (usuarioAutenticado.ehAdministrador()) {
            return repository.findAll();
        }

        Set<Long> simuladoIds = simuladoService.simuladoIdsVisiveis();
        if (simuladoIds.isEmpty()) {
            return List.of();
        }

        Set<Long> tentativaIds = simuladoAlunoRepository.findBySimulado_IdIn(simuladoIds).stream()
                .map(SimuladoAluno::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return tentativaIds.isEmpty() ? List.of() : repository.findBySimuladoAluno_IdIn(tentativaIds);
    }

    public List<QuestaoAluno> listarPorSimuladoAluno(Long simuladoAlunoId) {
        return repository.findBySimuladoAlunoId(simuladoAlunoId);
    }
}
