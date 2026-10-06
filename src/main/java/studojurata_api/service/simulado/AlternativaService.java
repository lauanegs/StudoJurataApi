package studojurata_api.service.simulado;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.model.simulado.Alternativa;
import studojurata_api.model.simulado.Questao;
import studojurata_api.model.enums.TipoQuestao;
import studojurata_api.repository.simulado.AlternativaRepository;

import studojurata_api.security.PerfilDeGestao;

import java.util.List;

/** Leitura e escrita de alternativas */
@Service
@RequiredArgsConstructor
public class AlternativaService {
    private final AlternativaRepository repository;
    private final studojurata_api.security.UsuarioAutenticado usuarioAutenticado;
    private final studojurata_api.security.EscopoProfessor escopoProfessor;

    /** 403 de perfil — mensagens do domínio de alternativa, preservadas */
    private static final String MENSAGEM_PERFIL_LISTAR =
            "Apenas professor ou administrador pode listar alternativas.";
    private static final String MENSAGEM_PERFIL_ESCRITA =
            "Apenas professor ou administrador pode alterar alternativas.";

    /** ADMINISTRADOR ve tudo; PROFESSOR ve apenas alternativas de questoes das disciplinas que leciona */
    public List<Alternativa> listar() {
        var usuario = usuarioAutenticado.atual();

        if (usuario.getTipoUsuario() == studojurata_api.model.enums.TipoUsuario.ADMINISTRADOR) {
            return repository.findAll();
        }
        PerfilDeGestao.exigir(usuario, MENSAGEM_PERFIL_LISTAR);

        Long professorId = usuario.getProfessor() != null ? usuario.getProfessor().getId() : null;
        var disciplinas = escopoProfessor.disciplinaIdsDoProfessor(professorId);
        return disciplinas.isEmpty() ? List.of() : repository.findByQuestao_Disciplina_IdIn(disciplinas);
    }

    /** Questões ALTERNATIVAS têm no máximo uma correta, pois a correção lê a única alternativa escolhida */
    @Transactional
    public Alternativa salvar(Alternativa obj) {
        PerfilDeGestao.exigir(usuarioAutenticado.atual(), MENSAGEM_PERFIL_ESCRITA);
        garantirEscritaNaQuestao(obj.getQuestao());

        validarCorretaUnica(obj, null);
        return repository.save(obj);
    }

    /** Edição de alternativa (PUT /alternativas/{id}) */
    @Transactional
    public Alternativa atualizar(Long id, Alternativa obj) {
        PerfilDeGestao.exigir(usuarioAutenticado.atual(), MENSAGEM_PERFIL_ESCRITA);

        Alternativa existente = buscar(id);
        Questao questaoAtual = existente.getQuestao();
        garantirEscritaNaQuestao(questaoAtual);

        Questao destino = obj.getQuestao() != null ? obj.getQuestao() : questaoAtual;
        if (!mesmaQuestao(destino, questaoAtual)) {
            garantirEscritaNaQuestao(destino);
        }

        obj.setId(id);
        obj.setQuestao(destino);
        validarCorretaUnica(obj, id);
        return repository.save(obj);
    }

    /** Alternativa existente por id; 404 quando não existe */
    private Alternativa buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Alternativa " + id + " não encontrada."));
    }

    /** Escrita restrita às questões das disciplinas que o professor leciona */
    private void garantirEscritaNaQuestao(Questao questao) {
        if (usuarioAutenticado.ehAdministrador()) {
            return;
        }

        Long disciplinaId = questao != null && questao.getDisciplina() != null
                ? questao.getDisciplina().getId()
                : null;
        if (disciplinaId == null
                || !escopoProfessor.disciplinaIdsDoProfessor(usuarioAutenticado.professorId()).contains(disciplinaId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Você só pode salvar alternativas de questões das disciplinas que leciona.");
        }
    }

    private static boolean mesmaQuestao(Questao a, Questao b) {
        return a != null && b != null && a.getId() != null && a.getId().equals(b.getId());
    }

    private void validarCorretaUnica(Alternativa obj, Long ignorarId) {
        if (!Boolean.TRUE.equals(obj.getCorreta()) || obj.getQuestao() == null || obj.getQuestao().getId() == null) {
            return;
        }
        if (obj.getQuestao().getTipo() == TipoQuestao.VERDADEIRO_FALSO) {
            return;
        }
        boolean existeOutraCorreta = repository.findByQuestaoIdAndCorretaTrue(obj.getQuestao().getId()).stream()
                .anyMatch(a -> ignorarId == null || !a.getId().equals(ignorarId));
        if (existeOutraCorreta) {
            throw new RegraNegocioException("Esta questão já possui uma alternativa marcada como correta.");
        }
    }
}
