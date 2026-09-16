package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.model.AulaConteudo;
import studojurata_api.model.ConteudoPlano;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.AulaConteudoRepository;
import studojurata_api.repository.ConteudoPlanoRepository;

import java.util.List;

/**
 * Correção 5.1 + caso extremo "Conteúdo removido" (Segunda Análise
 * Crítica): antes o controller excluía ConteudoPlano fisicamente,
 * inconsistente com o soft-delete já aplicado em SimuladoQuestao/
 * AulaConteudo para preservar QuestaoAluno já respondido. Agora deletar()
 * marca o conteúdo como INATIVO em vez de remover a linha.
 */
@Service
@RequiredArgsConstructor
public class ConteudoPlanoService {

    private final ConteudoPlanoRepository repository;
    private final AulaConteudoRepository aulaConteudoRepository;

    public List<ConteudoPlano> listar() { return repository.findAll(); }

    public ConteudoPlano buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conteúdo do plano " + id + " não encontrado."));
    }

    public ConteudoPlano salvar(ConteudoPlano obj) {
        if (obj.getStatus() == null) obj.setStatus(StatusAtivoInativo.ATIVO);
        return repository.save(obj);
    }

    public ConteudoPlano atualizar(Long id, ConteudoPlano obj) {
        obj.setId(id);
        return repository.save(obj);
    }

    /**
     * Recusa (409) inativar um conteúdo que já foi ministrado — vinculado a
     * uma Aula com `dataPublicacao` preenchida (pedido do usuário). Um
     * conteúdo nunca vinculado a nenhuma aula ministrada pode ser inativado
     * normalmente.
     */
    public void deletar(Long id) {
        ConteudoPlano conteudo = buscar(id);

        boolean jaMinistrado = aulaConteudoRepository.findByConteudoPlano_Id(id).stream()
                .map(AulaConteudo::getAula)
                .anyMatch(aula -> aula != null && aula.getDataPublicacao() != null);

        if (jaMinistrado) {
            throw new RegraNegocioException(
                    "Este conteúdo já foi ministrado em alguma aula e não pode ser inativado.");
        }

        conteudo.setStatus(StatusAtivoInativo.INATIVO);
        repository.save(conteudo);
    }
}
