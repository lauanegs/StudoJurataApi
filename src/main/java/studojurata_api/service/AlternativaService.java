package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.model.Alternativa;
import studojurata_api.model.enums.TipoQuestao;
import studojurata_api.repository.AlternativaRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlternativaService {

    private final AlternativaRepository repository;

    public List<Alternativa> listar() { return repository.findAll(); }

    public Alternativa buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Alternativa " + id + " não encontrada."));
    }

    /**
     * Garante no máximo uma alternativa correta por Questao — regra
     * indispensável para a correção automática das questões ALTERNATIVAS
     * (ver itens 2.4 e 7.1: SimuladoAlunoService.finalizar lê
     * alternativaEscolhida.getCorreta() da ÚNICA alternativa que o aluno
     * seleciona, então só faz sentido existir uma resposta certa).
     * <p>
     * Questões VERDADEIRO_FALSO são o caso previsto na "Tela novo simulado"
     * do Documento de Interfaces: o professor lista várias afirmações
     * (alternativas) e marca CADA UMA independentemente como Verdadeira ou
     * Falsa — não existe "a única correta" entre elas. A correção automática
     * continua funcionando sem nenhuma mudança: o aluno ainda escolhe UMA
     * alternativa, e o `correta` dela (agora podendo haver várias com
     * `correta=true` na mesma questão) decide se acertou.
     */
    @Transactional
    public Alternativa salvar(Alternativa obj) {
        validarCorretaUnica(obj, null);
        return repository.save(obj);
    }

    @Transactional
    public Alternativa atualizar(Long id, Alternativa obj) {
        obj.setId(id);
        validarCorretaUnica(obj, id);
        return repository.save(obj);
    }

    public void deletar(Long id) { repository.deleteById(id); }

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
