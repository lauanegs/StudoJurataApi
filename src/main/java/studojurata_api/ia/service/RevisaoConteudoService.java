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
 * Reforço adaptativo por repetição espaçada (item 1.5 da Análise Crítica,
 * sugestão aprovada), seguindo a teoria da curva de esquecimento citada no
 * TCC: a cada reforço, o intervalo até o próximo aumenta, afastando
 * progressivamente a revisão de conteúdos já dominados.
 *
 * Confirmado pelo usuário: intervalos fixos e crescentes (não mais 2ⁿ dias) —
 * 7 dias após o 1º reforço, 14 após o 2º, 3 meses após o 3º — e a partir do
 * 4º reforço o conteúdo é considerado dominado (NivelDominio.ALTO) e a
 * repetição espaçada para (dataProximoReforco fica null, e some sozinho das
 * consultas "devidos", que já ignoram data nula).
 *
 * Correção 8.1/8.2 da Segunda Análise Crítica: cada reforço registrado
 * também concede moedas de gamificação (mesma quantidade concedida por
 * simulado concluído), garantindo que quem revisa seja tão beneficiado
 * quanto quem acerta de primeira.
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
