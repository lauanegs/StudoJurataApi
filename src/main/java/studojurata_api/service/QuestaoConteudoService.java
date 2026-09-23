package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.ConteudoPlano;
import studojurata_api.model.Questao;
import studojurata_api.model.QuestaoConteudo;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.ConteudoPlanoRepository;
import studojurata_api.repository.QuestaoConteudoRepository;
import studojurata_api.repository.QuestaoRepository;

import studojurata_api.security.PerfilDeGestao;

import java.util.List;
import java.util.Set;

/**
 * Sem o vínculo questão-conteúdo, a questão fica invisível para o cálculo de
 * desempenho por conteúdo (RecomendacaoService). Só as questões geradas pela
 * IA recebem o vínculo automaticamente; as criadas pelo professor dependem
 * deste service. Exclusão física é aceitável porque o vínculo não guarda
 * histórico pedagógico (quem guarda é QuestaoAluno/SimuladoQuestao).
 *
 * <p>O vínculo cruza dois recursos com recortes próprios — a questão (disciplina
 * dela) e o conteúdo (disciplina do plano de ensino) — e por isso o C2.7d
 * valida as <b>duas</b> pontas contra o escopo do professor: questão própria com
 * conteúdo de disciplina alheia (ou o inverso) não entra no banco.
 */
@Service
@RequiredArgsConstructor
public class QuestaoConteudoService {

    private final QuestaoConteudoRepository repository;
    private final QuestaoRepository questaoRepository;
    private final ConteudoPlanoRepository conteudoPlanoRepository;
    private final studojurata_api.security.UsuarioAutenticado usuarioAutenticado;
    private final studojurata_api.security.EscopoProfessor escopoProfessor;

    /** 403 de perfil — mensagem do domínio de vínculo, preservada. */
    private static final String MENSAGEM_PERFIL_DE_GESTAO =
            "Apenas professor ou administrador pode alterar vínculos de conteúdo.";

    public List<QuestaoConteudo> listarPorQuestao(Long questaoId) { return repository.findByQuestao_Id(questaoId); }

    /**
     * Restringe pela disciplina, não pelo plano de ensino como em
     * AulaConteudo, porque a questão não tem turma.
     */
    @Transactional
    public QuestaoConteudo vincular(Long questaoId, Long conteudoPlanoId) {
        PerfilDeGestao.exigir(usuarioAutenticado.atual(), MENSAGEM_PERFIL_DE_GESTAO);

        Questao questao = buscarQuestao(questaoId);
        ConteudoPlano conteudoPlano = buscarConteudo(conteudoPlanoId);

        validarVinculo(questao, conteudoPlano);
        garantirConteudoAtivo(conteudoPlano);

        if (repository.existsByQuestao_IdAndConteudoPlano_Id(questaoId, conteudoPlanoId)) {
            throw new RegraNegocioException("Este conteúdo já está vinculado a esta questão.");
        }

        QuestaoConteudo questaoConteudo = new QuestaoConteudo();
        questaoConteudo.setQuestao(questao);
        questaoConteudo.setConteudoPlano(conteudoPlano);
        return repository.save(questaoConteudo);
    }

    @Transactional
    public void desvincular(Long questaoId, Long conteudoPlanoId) {
        PerfilDeGestao.exigir(usuarioAutenticado.atual(), MENSAGEM_PERFIL_DE_GESTAO);

        Questao questao = buscarQuestao(questaoId);
        ConteudoPlano conteudoPlano = buscarConteudo(conteudoPlanoId);

        // As mesmas validações do vínculo, menos o "ATIVO": remover o vínculo de
        // um conteúdo inativado depois da vinculação é limpeza legítima (a tela
        // do professor mostra o chip justamente para permitir a remoção).
        validarVinculo(questao, conteudoPlano);

        repository.deleteByQuestao_IdAndConteudoPlano_Id(questaoId, conteudoPlanoId);
    }

    /** Questão do vínculo; 404 quando não existe. */
    private Questao buscarQuestao(Long questaoId) {
        return questaoRepository.findById(questaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Questão " + questaoId + " não encontrada."));
    }

    /** Conteúdo do vínculo; 404 quando não existe. */
    private ConteudoPlano buscarConteudo(Long conteudoPlanoId) {
        return conteudoPlanoRepository.findById(conteudoPlanoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conteúdo " + conteudoPlanoId + " não encontrado."));
    }

    /**
     * Escopo das duas pontas, resolvido uma única vez e reusado. Questão sem
     * disciplina e conteúdo sem plano/turma/disciplina são exclusivos do
     * administrador; o ADMIN passa direto, sem consultar escopo.
     */
    private void garantirEscopoDoVinculo(Questao questao, ConteudoPlano conteudoPlano) {
        if (usuarioAutenticado.ehAdministrador()) {
            return;
        }

        Set<Long> disciplinas = escopoProfessor.disciplinaIdsDoProfessor(usuarioAutenticado.professorId());

        Long disciplinaDaQuestao = disciplinaDaQuestaoId(questao);
        if (disciplinaDaQuestao == null || !disciplinas.contains(disciplinaDaQuestao)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Você só pode alterar vínculos de questões das disciplinas que leciona.");
        }

        Long disciplinaDoConteudo = disciplinaDoConteudoId(conteudoPlano);
        if (disciplinaDoConteudo == null || !disciplinas.contains(disciplinaDoConteudo)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Você só pode alterar vínculos de conteúdos das disciplinas que leciona.");
        }
    }

    /**
     * Vínculo estruturalmente aceito: as duas pontas no escopo (quando for
     * professor) e na mesma disciplina — regra que também vale para o ADMIN, por
     * ser o que dá sentido ao dado.
     */
    private void validarVinculo(Questao questao, ConteudoPlano conteudoPlano) {
        garantirEscopoDoVinculo(questao, conteudoPlano);

        Long disciplinaDaQuestao = disciplinaDaQuestaoId(questao);
        if (disciplinaDaQuestao == null || !disciplinaDaQuestao.equals(disciplinaDoConteudoId(conteudoPlano))) {
            throw new RequisicaoInvalidaException(
                    "Só é possível vincular conteúdos da mesma disciplina da questão.");
        }
    }

    /** INATIVO é o soft-delete do conteúdo: não recebe vínculo novo. */
    private void garantirConteudoAtivo(ConteudoPlano conteudoPlano) {
        if (conteudoPlano.getStatus() != StatusAtivoInativo.ATIVO) {
            throw new RegraNegocioException("Conteúdo inativado não pode ser vinculado a questões.");
        }
    }

    private Long disciplinaDaQuestaoId(Questao questao) {
        return questao.getDisciplina() != null ? questao.getDisciplina().getId() : null;
    }

    /** A questão não tem turma; a disciplina do conteúdo vem do plano de ensino. */
    private Long disciplinaDoConteudoId(ConteudoPlano conteudoPlano) {
        return conteudoPlano.getPlanoEnsino() != null
                && conteudoPlano.getPlanoEnsino().getTurmaDisciplina() != null
                && conteudoPlano.getPlanoEnsino().getTurmaDisciplina().getDisciplina() != null
                ? conteudoPlano.getPlanoEnsino().getTurmaDisciplina().getDisciplina().getId()
                : null;
    }
}
