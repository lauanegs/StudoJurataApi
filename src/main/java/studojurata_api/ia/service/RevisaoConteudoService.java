package studojurata_api.ia.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import studojurata_api.ia.model.RevisaoConteudo;
import studojurata_api.ia.model.enums.NivelDominio;
import studojurata_api.ia.repository.RevisaoConteudoRepository;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.ConteudoPlanoRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Reforço adaptativo por repetição espaçada (item 1.5 da Análise Crítica,
 * sugestão aprovada), seguindo a teoria da curva de esquecimento citada no
 * TCC: a cada reforço, o intervalo até o próximo dobra (2^quantidadeReforcos
 * dias), afastando progressivamente a revisão de conteúdos já dominados e
 * mantendo frequente a de conteúdos ainda frágeis.
 */
@Service
@RequiredArgsConstructor
public class RevisaoConteudoService {

    private final RevisaoConteudoRepository repository;
    private final AlunoRepository alunoRepository;
    private final ConteudoPlanoRepository conteudoPlanoRepository;

    public List<RevisaoConteudo> listarPorAluno(Long alunoId) {
        return repository.findByAlunoId(alunoId);
    }

    /** Repetição espaçada devida hoje, em toda a base (uso administrativo/job). */
    public List<RevisaoConteudo> listarDevidosHoje() {
        return repository.findByDataProximoReforcoLessThanEqual(LocalDate.now());
    }

    public List<RevisaoConteudo> listarDevidosHojePorAluno(Long alunoId) {
        return repository.findByAlunoIdAndDataProximoReforcoLessThanEqual(alunoId, LocalDate.now());
    }

    /**
     * Registra que um reforço (revisão do conteúdo, seja por simulado
     * concluído, seja por contato manual registrado pelo professor) ocorreu
     * hoje, recalculando a próxima data de repetição espaçada por 2^n dias e
     * o nível de domínio estimado.
     */
    @Transactional
    public RevisaoConteudo registrarReforco(Long alunoId, Long conteudoPlanoId, NivelDominio nivelDominioObservado) {
        RevisaoConteudo revisao = repository.findByAlunoIdAndConteudoPlanoId(alunoId, conteudoPlanoId)
                .orElseGet(() -> criar(alunoId, conteudoPlanoId));

        int quantidade = revisao.getQuantidadeReforcos() == null ? 0 : revisao.getQuantidadeReforcos();
        quantidade++;

        LocalDate hoje = LocalDate.now();
        long diasAteProximo = (long) Math.pow(2, quantidade);

        revisao.setQuantidadeReforcos(quantidade);
        revisao.setDataUltimoReforco(hoje);
        revisao.setDataProximoReforco(hoje.plusDays(diasAteProximo));
        if (nivelDominioObservado != null) {
            revisao.setNivelDominio(nivelDominioObservado);
        }

        return repository.save(revisao);
    }

    private RevisaoConteudo criar(Long alunoId, Long conteudoPlanoId) {
        RevisaoConteudo revisao = new RevisaoConteudo();
        revisao.setAluno(alunoRepository.findById(alunoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno não encontrado.")));
        revisao.setConteudoPlano(conteudoPlanoRepository.findById(conteudoPlanoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conteúdo não encontrado.")));
        revisao.setQuantidadeReforcos(0);
        revisao.setNivelDominio(NivelDominio.BAIXO);
        return revisao;
    }
}
