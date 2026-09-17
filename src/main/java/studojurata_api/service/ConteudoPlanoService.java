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
     * Soft-delete, recusado quando o conteúdo já foi ministrado (vinculado a
     * uma aula com dataPublicacao).
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
