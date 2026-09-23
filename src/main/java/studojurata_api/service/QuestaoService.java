package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.model.Questao;
import studojurata_api.model.enums.OrigemQuestao;
import studojurata_api.model.enums.StatusQuestao;
import studojurata_api.repository.QuestaoRepository;

import studojurata_api.security.PerfilDeGestao;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestaoService {

    private final QuestaoRepository repository;
    private final studojurata_api.security.UsuarioAutenticado usuarioAutenticado;
    private final studojurata_api.security.EscopoProfessor escopoProfessor;

    /** 403 de perfil — mensagem do domínio de questão, preservada. */
    private static final String MENSAGEM_PERFIL_DE_GESTAO =
            "Apenas professor ou administrador pode acessar questoes.";

    /**
     * ADMINISTRADOR ve tudo; PROFESSOR ve as questoes das disciplinas que
     * leciona (questao sem disciplina fica exclusiva do administrador); demais
     * perfis recebem 403. Professor sem disciplinas nao consulta o repositorio.
     */
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

    /**
     * ADMIN passa; PROFESSOR so nas disciplinas que leciona — questao sem
     * disciplina e exclusiva do administrador. Demais perfis recebem 403.
     */
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

    /**
     * Criação manual de questão (POST /questoes).
     *
     * <p>Origem e status de moderação são do servidor, nunca do cliente: a
     * questão criada por este caminho é do professor e nasce APROVADA, como
     * sempre foi. As questões de IA nascem PENDENTE em
     * {@code GeracaoQuestaoIAService}, que não passa por este método — por isso
     * o cliente não tem como forjar origem nem status.
     *
     * <p>PROFESSOR só escreve nas disciplinas que leciona e a questão precisa
     * ter disciplina (sem disciplina ela é exclusiva do administrador, a mesma
     * regra do escopo de leitura); ADMIN continua livre.
     */
    public Questao salvar(Questao obj) {
        PerfilDeGestao.exigir(usuarioAutenticado.atual(), MENSAGEM_PERFIL_DE_GESTAO);
        garantirEscritaNaDisciplina(disciplinaIdDe(obj));

        obj.setOrigem(OrigemQuestao.PROFESSOR);
        obj.setStatus(StatusQuestao.APROVADA);
        return repository.save(obj);
    }

    /**
     * Edição de questão (PUT /questoes/{id}).
     *
     * <p>O escopo de leitura do C2.7a vale para a questão existente e para a
     * disciplina de destino, para que ninguém mova a questão para fora do
     * próprio escopo. Origem e status vêm sempre do registro atual: o status de
     * moderação só muda por {@code aprovar()}/{@code rejeitar()}.
     */
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

    @Transactional
    public Questao aprovar(Long id) {
        Questao questao = buscar(id);
        if (questao.getStatus() != StatusQuestao.PENDENTE) {
            throw new RegraNegocioException("Apenas questões PENDENTES podem ser aprovadas.");
        }
        questao.setStatus(StatusQuestao.APROVADA);
        return repository.save(questao);
    }

    @Transactional
    public Questao rejeitar(Long id) {
        Questao questao = buscar(id);
        if (questao.getStatus() != StatusQuestao.PENDENTE) {
            throw new RegraNegocioException("Apenas questões PENDENTES podem ser rejeitadas.");
        }
        questao.setStatus(StatusQuestao.REJEITADA);
        return repository.save(questao);
    }

    /** Disciplina do corpo enviado; nula quando o cliente não informou nenhuma. */
    private Long disciplinaIdDe(Questao questao) {
        return questao.getDisciplina() != null ? questao.getDisciplina().getId() : null;
    }

    /**
     * Escrita restrita às disciplinas que o professor leciona — a mesma fonte de
     * escopo da leitura ({@code EscopoProfessor.disciplinaIdsDoProfessor}), com
     * uma única consulta e sem tocar no repositório de questões. O ADMIN passa
     * direto, sem consultar escopo.
     */
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
