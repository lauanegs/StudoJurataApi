package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studojurata_api.model.SimuladoQuestao;
import studojurata_api.model.enums.StatusSimuladoQuestao;
import studojurata_api.repository.QuestaoConteudoRepository;
import studojurata_api.repository.SimuladoQuestaoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SimuladoQuestaoService {

    private final SimuladoQuestaoRepository repository;
    private final QuestaoConteudoRepository questaoConteudoRepository;

    public List<SimuladoQuestao> listar() { return repository.findAll(); }

    public SimuladoQuestao buscar(Long id) { return repository.findById(id).orElseThrow(); }

    /**
     * Ver item 7.1 da Análise Crítica: toda questão usada em um simulado
     * precisa estar vinculada a um conteúdo (QuestaoConteudo) — pré-requisito
     * estrutural para qualquer geração/adaptação futura baseada em conteúdo.
     */
    @Transactional
    public SimuladoQuestao salvar(SimuladoQuestao obj) {
        validarQuestaoVinculadaAoConteudo(obj);
        if (obj.getStatus() == null) {
            obj.setStatus(StatusSimuladoQuestao.ATIVA);
        }
        return repository.save(obj);
    }

    @Transactional
    public SimuladoQuestao atualizar(Long id, SimuladoQuestao obj) {
        obj.setId(id);
        validarQuestaoVinculadaAoConteudo(obj);
        return repository.save(obj);
    }

    /**
     * Remove (soft-delete) a questão do simulado — status REMOVIDA — em vez
     * de excluir fisicamente, preservando o histórico de respostas
     * (QuestaoAluno) já registradas contra ela. Ver Casos Extremos:
     * "Conteudo removido" e "Plano de ensino alterado após simulados já
     * realizados".
     */
    @Transactional
    public SimuladoQuestao remover(Long id) {
        SimuladoQuestao simuladoQuestao = buscar(id);
        simuladoQuestao.setStatus(StatusSimuladoQuestao.REMOVIDA);
        return repository.save(simuladoQuestao);
    }

    /**
     * Exclusão física: mantida apenas para compatibilidade/uso administrativo
     * pontual. Preferir sempre remover() para preservar histórico.
     */
    public void deletar(Long id) { repository.deleteById(id); }

    private void validarQuestaoVinculadaAoConteudo(SimuladoQuestao obj) {
        if (obj.getQuestao() == null || obj.getQuestao().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Questão é obrigatória.");
        }
        if (!questaoConteudoRepository.existsByQuestaoId(obj.getQuestao().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A questão precisa estar vinculada a um conteúdo antes de compor um simulado.");
        }
    }
}
