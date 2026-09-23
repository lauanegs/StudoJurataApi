package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import studojurata_api.security.PlanejamentoAccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.Aula;
import studojurata_api.model.AulaConteudo;
import studojurata_api.model.ConteudoPlano;
import studojurata_api.repository.AulaConteudoRepository;
import studojurata_api.repository.AulaRepository;
import studojurata_api.repository.ConteudoPlanoRepository;

import java.util.List;

/**
 * Só é permitido vincular conteúdos do mesmo PlanoEnsino do PlanoAula da
 * aula: cada turma tem o próprio plano de ensino, e um conteúdo de outro
 * plano não faz parte do que foi planejado para aquela turma.
 */
@Service
@RequiredArgsConstructor
public class AulaConteudoService {

    private final AulaConteudoRepository repository;
    private final AulaRepository aulaRepository;
    private final ConteudoPlanoRepository conteudoPlanoRepository;
    private final PlanejamentoAccessGuard planejamentoAccessGuard;

    public List<AulaConteudo> listarPorAula(Long aulaId) {
        Aula aula = buscarAula(aulaId);
        planejamentoAccessGuard.garantirLeitura(vinculoId(aula),
                "Você só pode acessar conteúdos das aulas das suas turmas.");
        return repository.findByAula_Id(aulaId);
    }

    @Transactional
    public AulaConteudo vincular(Long aulaId, Long conteudoPlanoId) {
        Aula aula = buscarAula(aulaId);
        planejamentoAccessGuard.garantirEscrita(vinculoId(aula),
                "Você só pode vincular conteúdos às aulas das suas turmas.");
        ConteudoPlano conteudoPlano = conteudoPlanoRepository.findById(conteudoPlanoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conteúdo " + conteudoPlanoId + " não encontrado."));

        Long planoEnsinoDaAulaId = aula.getPlanoAula().getPlanoEnsino().getId();
        Long planoEnsinoDoConteudoId = conteudoPlano.getPlanoEnsino() != null ? conteudoPlano.getPlanoEnsino().getId() : null;
        if (!planoEnsinoDaAulaId.equals(planoEnsinoDoConteudoId)) {
            throw new RequisicaoInvalidaException(
                    "Só é possível vincular conteúdos do plano de ensino vinculado a esta aula.");
        }

        if (repository.existsByAula_IdAndConteudoPlano_Id(aulaId, conteudoPlanoId)) {
            throw new RegraNegocioException("Este conteúdo já está vinculado a esta aula.");
        }

        AulaConteudo aulaConteudo = new AulaConteudo();
        aulaConteudo.setAula(aula);
        aulaConteudo.setConteudoPlano(conteudoPlano);
        return repository.save(aulaConteudo);
    }

    @Transactional
    public void desvincular(Long aulaId, Long conteudoPlanoId) {
        Aula aula = buscarAula(aulaId);
        planejamentoAccessGuard.garantirEscrita(vinculoId(aula),
                "Você só pode desvincular conteúdos das aulas das suas turmas.");
        repository.deleteByAula_IdAndConteudoPlano_Id(aulaId, conteudoPlanoId);
    }

    private Aula buscarAula(Long aulaId) {
        return aulaRepository.findById(aulaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aula " + aulaId + " não encontrada."));
    }

    private static Long vinculoId(Aula aula) {
        return aula.getPlanoAula() != null && aula.getPlanoAula().getTurmaDisciplina() != null
                ? aula.getPlanoAula().getTurmaDisciplina().getId()
                : null;
    }
}
