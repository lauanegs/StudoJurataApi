package studojurata_api.service.simulado;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.model.simulado.Questao;
import studojurata_api.model.enums.OrigemQuestao;
import studojurata_api.model.enums.StatusQuestao;
import studojurata_api.repository.simulado.QuestaoRepository;

import studojurata_api.security.PerfilDeGestao;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestaoService {
    private final QuestaoRepository repository;
    private final studojurata_api.security.UsuarioAutenticado usuarioAutenticado;
    private final studojurata_api.security.EscopoProfessor escopoProfessor;

    /** 403 de perfil — mensagem do domínio de questão, preservada */
    private static final String MENSAGEM_PERFIL_DE_GESTAO =
            "Apenas professor ou administrador pode acessar questoes.";

    /** ADMINISTRADOR ve tudo; PROFESSOR ve as questoes das disciplinas que leciona */
    public List<Questao> listar() {
        PerfilDeGestao.exigir(usuarioAutenticado.atual(), MENSAGEM_PERFIL_DE_GESTAO);

        if (usuarioAutenticado.ehAdministrador()) {
            return repository.findAll();
        }

        var disciplinas = escopoProfessor.disciplinaIdsDoProfessor(professorLogadoId());
        return disciplinas.isEmpty() ? List.of() : repository.findByDisciplina_IdIn(disciplinas);
    }

    public Questao buscar(Long id) {
        Questao questao = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Questão " + id + " não encontrada."));
        garantirAcessoAQuestao(questao);
        return questao;
    }

    private Long professorLogadoId() {
        var usuario = usuarioAutenticado.atual();
        return usuario.getProfessor() != null ? usuario.getProfessor().getId() : null;
    }

    /** ADMIN passa; PROFESSOR so nas disciplinas que leciona */
    private void garantirAcessoAQuestao(Questao questao) {
        PerfilDeGestao.exigir(usuarioAutenticado.atual(), MENSAGEM_PERFIL_DE_GESTAO);

        if (usuarioAutenticado.ehAdministrador()) {
            return;
        }

        Long disciplinaId = questao.getDisciplina() != null ? questao.getDisciplina().getId() : null;
        if (disciplinaId == null
                || !escopoProfessor.disciplinaIdsDoProfessor(professorLogadoId()).contains(disciplinaId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Você só pode acessar questões das disciplinas que leciona.");
        }
    }

    /** Criação manual de questão (POST /questoes) */
    public Questao salvar(Questao obj) {
        PerfilDeGestao.exigir(usuarioAutenticado.atual(), MENSAGEM_PERFIL_DE_GESTAO);
        garantirEscritaNaDisciplina(disciplinaIdDe(obj));

        obj.setOrigem(OrigemQuestao.PROFESSOR);
        obj.setStatus(StatusQuestao.APROVADA);
        return repository.save(obj);
    }

    /** Edição de questão (PUT /questoes/{id}) */
    public Questao atualizar(Long id, Questao obj) {
        Questao existente = buscar(id);
        garantirEscritaNaDisciplina(disciplinaIdDe(obj));

        obj.setId(id);
        obj.setOrigem(existente.getOrigem());
        obj.setStatus(existente.getStatus());
        return repository.save(obj);
    }

    public List<Questao> listarPendentes() {
        PerfilDeGestao.exigir(usuarioAutenticado.atual(), MENSAGEM_PERFIL_DE_GESTAO);

        if (usuarioAutenticado.ehAdministrador()) {
            return repository.findByStatus(StatusQuestao.PENDENTE);
        }

        var disciplinas = escopoProfessor.disciplinaIdsDoProfessor(professorLogadoId());
        return disciplinas.isEmpty() ? List.of()
                : repository.findByStatusAndDisciplina_IdIn(StatusQuestao.PENDENTE, disciplinas);
    }

    /** Pendente ou rejeitada pode ser aprovada */
    @Transactional
    public Questao aprovar(Long id) {
        Questao questao = buscar(id);
        if (questao.getStatus() == StatusQuestao.APROVADA) {
            throw new RegraNegocioException("A questão já está aprovada.");
        }
        questao.setStatus(StatusQuestao.APROVADA);
        return repository.save(questao);
    }

    /** Pendente ou aprovada pode ser rejeitada */
    @Transactional
    public Questao rejeitar(Long id) {
        Questao questao = buscar(id);
        if (questao.getStatus() == StatusQuestao.REJEITADA) {
            throw new RegraNegocioException("A questão já está rejeitada.");
        }
        questao.setStatus(StatusQuestao.REJEITADA);
        return repository.save(questao);
    }

    /** Disciplina do corpo enviado; nula quando o cliente não informou nenhuma */
    private Long disciplinaIdDe(Questao questao) {
        return questao.getDisciplina() != null ? questao.getDisciplina().getId() : null;
    }

    /** Escrita restrita às disciplinas que o professor leciona */
    private void garantirEscritaNaDisciplina(Long disciplinaId) {
        if (usuarioAutenticado.ehAdministrador()) {
            return;
        }
        if (disciplinaId == null
                || !escopoProfessor.disciplinaIdsDoProfessor(professorLogadoId()).contains(disciplinaId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Você só pode salvar questões das disciplinas que leciona.");
        }
    }
}
