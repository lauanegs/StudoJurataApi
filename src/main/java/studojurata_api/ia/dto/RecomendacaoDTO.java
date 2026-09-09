package studojurata_api.ia.dto;

import lombok.Getter;
import lombok.Setter;
import studojurata_api.ia.model.enums.MotivoRecomendacao;
import studojurata_api.model.enums.NivelDificuldade;

import java.time.LocalDate;
import java.util.Set;

/**
 * Recomendação de reforço para um aluno em um conteúdo específico (ver
 * RecomendacaoService, itens 1.4/1.5/7.4 da Análise Crítica). Não dispara
 * nada sozinha: é a entrada para o professor (ou para um job futuro) decidir
 * acionar GeracaoSimuladoIAService.
 */
@Getter
@Setter
public class RecomendacaoDTO {
    private Long alunoId;
    private Long conteudoPlanoId;
    private String conteudoTitulo;
    private Set<MotivoRecomendacao> motivos;
    /** Taxa de acerto do aluno nas questões desse conteúdo (0 a 1), quando aplicável. */
    private Double taxaAcerto;
    /**
     * Só presente quando o motivo é BAIXO_APROVEITAMENTO: o nível de
     * dificuldade mais baixo em que o aluno está abaixo do limiar daquele
     * nível (ver RecomendacaoService.sugerirNivelReforco) — é o nível que
     * GeracaoSimuladoIAService usa pra montar o simulado de reforço, salvo
     * quando o professor força um nível manualmente.
     */
    private NivelDificuldade nivelPrioritario;
    /** Data em que a repetição espaçada ficou devida, quando aplicável. */
    private LocalDate dataProximoReforco;
}
