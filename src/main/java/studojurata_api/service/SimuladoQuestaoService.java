package studojurata_api.service;

import studojurata_api.model.enums.TipoUsuario;
import studojurata_api.security.UsuarioAutenticado;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.SimuladoQuestao;
import studojurata_api.model.enums.OrigemQuestao;
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
     * O vínculo com conteúdo só é exigido para questões de origem IA, que já
     * nascem vinculadas. Para as do professor o vínculo é opcional, para não
     * travar a criação manual de simulados.
     */
    @Transactional
    public SimuladoQuestao salvar(SimuladoQuestao obj) {
        validarQuestaoVinculadaAoConteudo(obj);
        validarLimiteDeQuestoes(obj);
        if (obj.getStatus() == null) {
            obj.setStatus(StatusSimuladoQuestao.ATIVA);
        }
        return repository.save(obj);
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
