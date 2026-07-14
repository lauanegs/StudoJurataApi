package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studojurata_api.model.Aula;
import studojurata_api.model.AulaConteudo;
import studojurata_api.model.ConteudoPlano;
import studojurata_api.repository.AulaConteudoRepository;
import studojurata_api.repository.AulaRepository;
import studojurata_api.repository.ConteudoPlanoRepository;

import java.util.List;

/**
 * Vincula/desvincula conteúdos do plano de ensino a uma aula (tela
 * "Registrar conteúdo" / modal "Vincular conteúdo").
 *
 * Conforme a nota da interface ("cada turma pode ter um plano de ensino,
 * então nesse modal aparecerá os conteúdos apenas do plano de ensino para a
 * turma selecionada naquele plano de aula"), só é permitido vincular
 * conteúdos que pertençam ao mesmo PlanoEnsino do PlanoAula da aula.
 */
@Service
@RequiredArgsConstructor
public class AulaConteudoService {

    private final AulaConteudoRepository repository;
    private final AulaRepository aulaRepository;
    private final ConteudoPlanoRepository conteudoPlanoRepository;

    public List<AulaConteudo> listar() { return repository.findAll(); }

    public AulaConteudo buscar(Long id) { return repository.findById(id).orElseThrow(); }

    public List<AulaConteudo> listarPorAula(Long aulaId) { return repository.findByAula_Id(aulaId); }

    @Transactional
    public AulaConteudo vincular(Long aulaId, Long conteudoPlanoId) {
        Aula aula = aulaRepository.findById(aulaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aula não encontrada."));
        ConteudoPlano conteudoPlano = conteudoPlanoRepository.findById(conteudoPlanoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conteúdo não encontrado."));

        Long planoEnsinoDaAulaId = aula.getPlanoAula().getPlanoEnsino().getId();
        Long planoEnsinoDoConteudoId = conteudoPlano.getPlanoEnsino() != null ? conteudoPlano.getPlanoEnsino().getId() : null;
        if (!planoEnsinoDaAulaId.equals(planoEnsinoDoConteudoId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Só é possível vincular conteúdos do plano de ensino vinculado a esta aula.");
        }

        if (repository.existsByAula_IdAndConteudoPlano_Id(aulaId, conteudoPlanoId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este conteúdo já está vinculado a esta aula.");
        }

        AulaConteudo aulaConteudo = new AulaConteudo();
        aulaConteudo.setAula(aula);
        aulaConteudo.setConteudoPlano(conteudoPlano);
        return repository.save(aulaConteudo);
    }

    @Transactional
    public void desvincular(Long aulaId, Long conteudoPlanoId) {
        repository.deleteByAula_IdAndConteudoPlano_Id(aulaId, conteudoPlanoId);
    }

    @Transactional
    public void deletar(Long id) { repository.deleteById(id); }
}
