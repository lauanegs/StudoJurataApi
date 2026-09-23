package studojurata_api.service;

import studojurata_api.security.SimuladoAccessGuard;
import studojurata_api.security.UsuarioAutenticado;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.Disciplina;
import studojurata_api.model.Questao;
import studojurata_api.model.Simulado;
import studojurata_api.model.SimuladoQuestao;
import studojurata_api.model.enums.OrigemQuestao;
import studojurata_api.model.enums.StatusSimulado;
import studojurata_api.model.enums.StatusSimuladoQuestao;
import studojurata_api.repository.QuestaoConteudoRepository;
import studojurata_api.repository.SimuladoQuestaoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SimuladoQuestaoService {

    /** Regra de negócio: um simulado nunca pode ter mais que 10 questões. */
    private static final int MAXIMO_QUESTOES_POR_SIMULADO = 10;

    private final SimuladoQuestaoRepository repository;
    private final QuestaoConteudoRepository questaoConteudoRepository;
    private final UsuarioAutenticado usuarioAutenticado;
    private final SimuladoService simuladoService;
    private final SimuladoAccessGuard simuladoAccessGuard;

    /**
     * Listagem escopada: ADMINISTRADOR ve todos os vinculos; PROFESSOR e ALUNO
     * veem apenas os vinculos dos simulados que podem acessar (para o aluno, os
     * simulados em que tem tentativa). Orfaos preservados por D-C1.
     */
    public List<SimuladoQuestao> listar() {
        if (usuarioAutenticado.ehAdministrador()) {
            return repository.findAll();
        }

        var simuladoIds = simuladoService.simuladoIdsVisiveis();
        return simuladoIds.isEmpty() ? List.of() : repository.findBySimulado_IdIn(simuladoIds);
    }

    /**
     * Vincula questão a simulado (POST /simulado-questao).
     *
     * <p>Além das regras que já existiam — a questão de IA precisa estar
     * vinculada a um conteúdo e o simulado nunca passa de 10 questões —, o
     * C2.7d valida as <b>duas</b> pontas contra o escopo do professor: o
     * simulado (turma, disciplina e plano) e a questão (disciplina). O cliente só
     * informa ids, e as entidades chegam do banco pelo mapper; nada aqui confia
     * no conteúdo do corpo além desses ids.
     */
    @Transactional
    public SimuladoQuestao salvar(SimuladoQuestao obj) {
        Simulado simulado = obj.getSimulado();
        Questao questao = obj.getQuestao();

        // Escopo das duas pontas: simulado (turma/disciplina/plano) e questão
        // (disciplina), com uma única resolução de escopo do professor.
        simuladoAccessGuard.garantirEscritaDoVinculo(simulado, questao);
        exigirSimuladoEmRascunho(simulado);

        validarQuestaoVinculadaAoConteudo(obj);
        validarCoerenciaDeDisciplinas(simulado, questao);
        validarLimiteDeQuestoes(obj);
        validarVinculoAtivoUnico(obj);
        if (obj.getStatus() == null) {
            obj.setStatus(StatusSimuladoQuestao.ATIVA);
        }
        SimuladoQuestao salvo = repository.save(obj);
        // A quantidade do simulado é sempre a contagem real de vínculos ativos.
        simuladoService.sincronizarQuantidadeDeQuestoes(salvo.getSimulado());
        return salvo;
    }

    /**
     * A mesma questão não entra duas vezes no simulado: dois vínculos levariam a
     * questão repetida na prova e a contagem inflada. Vínculo REMOVIDA não conta
     * — a questão pode voltar depois de sair.
     */
    private void validarVinculoAtivoUnico(SimuladoQuestao obj) {
        Long simuladoId = obj.getSimulado() != null ? obj.getSimulado().getId() : null;
        Long questaoId = obj.getQuestao() != null ? obj.getQuestao().getId() : null;
        if (simuladoId == null || questaoId == null) {
            return;
        }
        if (repository.existsBySimuladoIdAndQuestaoIdAndStatus(simuladoId, questaoId, StatusSimuladoQuestao.ATIVA)) {
            throw new RegraNegocioException("Esta questão já está vinculada a este simulado.");
        }
    }

    /**
     * O simulado em RASCUNHO é o único que aceita novas questões: depois de
     * lançado já existem tentativas (e possivelmente alunos respondendo), então
     * vincular questão mudaria a prova em andamento. A tela já bloqueia esse
     * caminho; aqui a mesma regra vale para quem chama a API direto.
     */
    private void exigirSimuladoEmRascunho(Simulado simulado) {
        if (simulado == null || simulado.getId() == null) {
            throw new RequisicaoInvalidaException("Simulado é obrigatório.");
        }
        if (simulado.getStatus() != StatusSimulado.RASCUNHO) {
            throw new RegraNegocioException(
                    "Só é possível vincular questões a um simulado em RASCUNHO.");
        }
    }

    /**
     * Tira a questão do simulado (DELETE
     * /simulado-questao/simulado/{id}/questao/{id}). A questão continua
     * existindo — o que sai é só o relacionamento com este simulado.
     *
     * <p>Mesma porta da inclusão: perfil e escopo pelo
     * {@link SimuladoAccessGuard} e simulado em RASCUNHO. Depois de publicado já
     * existem tentativas (e possivelmente respostas), e mexer na prova em
     * andamento mudaria o simulado de quem está respondendo.
     *
     * <p>Apaga o vínculo, como nos outros desvínculos do domínio
     * ({@code QuestaoConteudoService}/{@code AulaConteudoService}): em RASCUNHO
     * não há resposta registrada para preservar.
     */
    @Transactional
    public void desvincular(Long simuladoId, Long questaoId) {
        Simulado simulado = simuladoService.buscar(simuladoId);
        simuladoAccessGuard.garantirEscrita(simulado);
        exigirSimuladoEmRascunho(simulado);

        SimuladoQuestao vinculo = repository.findFirstBySimuladoIdAndQuestaoId(simuladoId, questaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Esta questão não está vinculada a este simulado."));

        repository.delete(vinculo);
        simuladoService.sincronizarQuantidadeDeQuestoes(simulado);
    }

    /**
     * O simulado não mistura disciplinas: a questão precisa ser da mesma
     * disciplina dele, senão o desempenho por conteúdo perde o sentido. Vale
     * também para o ADMIN, por ser regra do dado e não de escopo. A questão aqui
     * já é não nula: {@code validarQuestaoVinculadaAoConteudo} roda antes.
     */
    private void validarCoerenciaDeDisciplinas(Simulado simulado, Questao questao) {
        if (simulado == null || simulado.getId() == null) {
            throw new RequisicaoInvalidaException("Simulado é obrigatório.");
        }
        Long disciplinaDaQuestao = disciplinaIdDe(questao.getDisciplina());
        if (disciplinaDaQuestao == null
                || !disciplinaDaQuestao.equals(disciplinaIdDe(simulado.getDisciplina()))) {
            throw new RequisicaoInvalidaException(
                    "Só é possível vincular questões da mesma disciplina do simulado.");
        }
    }

    private Long disciplinaIdDe(Disciplina disciplina) {
        return disciplina != null ? disciplina.getId() : null;
    }

    private void validarLimiteDeQuestoes(SimuladoQuestao obj) {
        if (obj.getSimulado() == null || obj.getSimulado().getId() == null) return;

        long ativas = repository.countBySimuladoIdAndStatus(obj.getSimulado().getId(), StatusSimuladoQuestao.ATIVA);
        if (ativas >= MAXIMO_QUESTOES_POR_SIMULADO) {
            throw new RegraNegocioException(
                    "Este simulado já tem o máximo de " + MAXIMO_QUESTOES_POR_SIMULADO + " questões.");
        }
    }

    private void validarQuestaoVinculadaAoConteudo(SimuladoQuestao obj) {
        if (obj.getQuestao() == null || obj.getQuestao().getId() == null) {
            throw new RequisicaoInvalidaException("Questão é obrigatória.");
        }
        if (obj.getQuestao().getOrigem() == OrigemQuestao.PROFESSOR) {
            return;
        }
        if (!questaoConteudoRepository.existsByQuestaoId(obj.getQuestao().getId())) {
            throw new RegraNegocioException(
                    "A questão precisa estar vinculada a um conteúdo antes de compor um simulado.");
        }
    }
}
