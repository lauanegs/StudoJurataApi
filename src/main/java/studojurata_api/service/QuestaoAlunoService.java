package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.model.QuestaoAluno;
import studojurata_api.model.SimuladoAluno;
import studojurata_api.repository.QuestaoAlunoRepository;
import studojurata_api.repository.SimuladoAlunoRepository;
import studojurata_api.security.UsuarioAutenticado;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * O caminho principal de escrita das respostas é SimuladoAlunoService.finalizar;
 * este service atende consultas pontuais.
 */
@Service
@RequiredArgsConstructor
public class QuestaoAlunoService {

    private final QuestaoAlunoRepository repository;
    private final SimuladoAlunoRepository simuladoAlunoRepository;
    private final SimuladoService simuladoService;
    private final UsuarioAutenticado usuarioAutenticado;

    /**
     * Listagem escopada: ADMINISTRADOR ve todas as respostas; os demais perfis veem
     * apenas as respostas das tentativas dos simulados que podem acessar - para o
     * professor, os simulados das turmas em que leciona (mesma fonte de
     * {@link SimuladoService#simuladoIdsVisiveis()}). O aluno nao chega aqui: o
     * controller exige perfil de gestao antes de consultar.
     *
     * <p>O recorte e obrigatorio no servidor porque a resposta carrega a alternativa
     * marcada e se o aluno acertou: a combinacao revela o gabarito da questao.
     */
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
