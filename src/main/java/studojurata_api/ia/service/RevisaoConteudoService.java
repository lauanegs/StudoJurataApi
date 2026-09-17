package studojurata_api.ia.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.ia.model.RevisaoConteudo;
import studojurata_api.ia.model.enums.NivelDominio;
import studojurata_api.ia.repository.RevisaoConteudoRepository;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.ConteudoPlanoRepository;
import studojurata_api.service.gamificacao.PontuacaoAlunoService;

import java.time.LocalDate;
import java.util.List;

/**
 * Repetição espaçada (ver RevisaoConteudo para os intervalos). Cada reforço
 * concede as mesmas moedas de um simulado concluído, para que revisar seja
 * tão recompensado quanto acertar de primeira.
 */
@Service
@RequiredArgsConstructor
public class RevisaoConteudoService {

    /** Reforços com intervalo agendado — o 4º encerra a repetição espaçada (conteúdo dominado). */
    private static final int LIMITE_REFORCOS_AGENDADOS = 3;

    private final RevisaoConteudoRepository repository;
    private final AlunoRepository alunoRepository;
    private final ConteudoPlanoRepository conteudoPlanoRepository;
    private final PontuacaoAlunoService pontuacaoAlunoService;

    public List<RevisaoConteudo> listarPorAluno(Long alunoId) {
        return repository.findByAlunoId(alunoId);
    }

    /**
     * Registra um reforço feito hoje (simulado concluído ou revisão manual),
     * recalculando a próxima data e o nível de domínio.
     */
    @Transactional
    public RevisaoConteudo registrarReforco(Long alunoId, Long conteudoPlanoId, NivelDominio nivelDominioObservado) {
        RevisaoConteudo revisao = repository.findByAlunoIdAndConteudoPlanoId(alunoId, conteudoPlanoId)
                .orElseGet(() -> criar(alunoId, conteudoPlanoId));

        int quantidade = revisao.getQuantidadeReforcos() == null ? 0 : revisao.getQuantidadeReforcos();
        quantidade++;

        LocalDate hoje = LocalDate.now();
        boolean dominado = quantidade > LIMITE_REFORCOS_AGENDADOS;

        revisao.setQuantidadeReforcos(quantidade);
        revisao.setDataUltimoReforco(hoje);
        // Dominado: para de agendar (null some sozinho das consultas "devidos").
        revisao.setDataProximoReforco(dominado ? null : hoje.plusDays(diasAteProximoReforco(quantidade)));
        revisao.setNivelDominio(dominado ? NivelDominio.ALTO : nivelDominioObservado != null ? nivelDominioObservado : revisao.getNivelDominio());

        RevisaoConteudo salva = repository.save(revisao);

        pontuacaoAlunoService.concederMoedas(alunoId, PontuacaoAlunoService.MOEDAS_POR_REFORCO);

        return salva;
    }

    /** 7 dias após o 1º reforço, 14 após o 2º, 3 meses (90 dias) após o 3º. */
    private static long diasAteProximoReforco(int quantidadeReforcos) {
        return switch (quantidadeReforcos) {
            case 1 -> 7L;
            case 2 -> 14L;
            default -> 90L;
        };
    }

    private RevisaoConteudo criar(Long alunoId, Long conteudoPlanoId) {
        RevisaoConteudo revisao = new RevisaoConteudo();
        revisao.setAluno(alunoRepository.findById(alunoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado.")));
        revisao.setConteudoPlano(conteudoPlanoRepository.findById(conteudoPlanoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conteúdo não encontrado.")));
        revisao.setQuantidadeReforcos(0);
        revisao.setNivelDominio(NivelDominio.BAIXO);
        return revisao;
    }
}
