package studojurata_api.ia.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import studojurata_api.exception.RegraNegocioException;
import studojurata_api.ia.model.RevisaoConteudo;
import studojurata_api.ia.model.enums.MotivoRecomendacao;
import studojurata_api.ia.repository.SimuladoGeradoIARepository;
import studojurata_api.ia.repository.RevisaoConteudoRepository;
import studojurata_api.ia.service.GeracaoSimuladoIAService;
import studojurata_api.model.Aluno;
import studojurata_api.model.ConteudoPlano;
import studojurata_api.model.Simulado;

/**
 * Job de reforço automático (Fase 1 do M8): recusa de negócio — conteúdo
 * comprovadamente futuro, por exemplo — é registrada e o laço segue para os
 * demais itens, como já acontece com o alerta coletivo.
 */
@ExtendWith(MockitoExtension.class)
class C2GeracaoAutomaticaSimuladoJobTest {

    private static final LocalDate PRAZO = LocalDate.now();

    @Mock private RevisaoConteudoRepository revisaoConteudoRepository;
    @Mock private SimuladoGeradoIARepository simuladoGeradoIARepository;
    @Mock private GeracaoSimuladoIAService geracaoSimuladoIAService;

    @Test
    @DisplayName("recusa por conteúdo futuro não interrompe o processamento dos demais")
    void recusaDeConteudoFuturoNaoInterrompeOJob() {
        RevisaoConteudo primeiro = revisao(1L, 100L);
        RevisaoConteudo segundo = revisao(2L, 200L);
        given(revisaoConteudoRepository.findByDataProximoReforcoLessThanEqual(any(LocalDate.class)))
                .willReturn(List.of(primeiro, segundo));

        willThrow(new RegraNegocioException(
                "Este conteúdo está planejado para o futuro e ainda não foi ministrado."))
                .given(geracaoSimuladoIAService)
                .gerarParaAluno(1L, 100L, null, Set.of(MotivoRecomendacao.REPETICAO_ESPACADA));
        given(geracaoSimuladoIAService.gerarParaAluno(2L, 200L, null, Set.of(MotivoRecomendacao.REPETICAO_ESPACADA)))
                .willReturn(new Simulado());

        // Não propaga a recusa: se propagasse, o teste falharia aqui.
        job().gerarSimuladosDevidos();

        verify(geracaoSimuladoIAService).gerarParaAluno(1L, 100L, null, Set.of(MotivoRecomendacao.REPETICAO_ESPACADA));
        verify(geracaoSimuladoIAService).gerarParaAluno(2L, 200L, null, Set.of(MotivoRecomendacao.REPETICAO_ESPACADA));
        verify(geracaoSimuladoIAService, times(2)).gerarParaAluno(any(), any(), any(), any());
    }

    @Test
    @DisplayName("item já gerado no prazo é pulado sem chamar a geração")
    void itemJaGeradoEhPulado() {
        given(revisaoConteudoRepository.findByDataProximoReforcoLessThanEqual(any(LocalDate.class)))
                .willReturn(List.of(revisao(1L, 100L)));
        given(simuladoGeradoIARepository.existsByAlunoIdAndConteudoPlanoIdAndPrazoLancamento(1L, 100L, PRAZO))
                .willReturn(true);

        job().gerarSimuladosDevidos();

        verify(geracaoSimuladoIAService, never()).gerarParaAluno(any(), any(), any(), any());
    }

    private GeracaoAutomaticaSimuladoJob job() {
        return new GeracaoAutomaticaSimuladoJob(revisaoConteudoRepository, simuladoGeradoIARepository,
                geracaoSimuladoIAService);
    }

    private static RevisaoConteudo revisao(long alunoId, long conteudoPlanoId) {
        Aluno aluno = new Aluno();
        aluno.setId(alunoId);

        ConteudoPlano conteudo = new ConteudoPlano();
        conteudo.setId(conteudoPlanoId);

        RevisaoConteudo revisao = new RevisaoConteudo();
        revisao.setId(alunoId);
        revisao.setAluno(aluno);
        revisao.setConteudoPlano(conteudo);
        revisao.setDataProximoReforco(PRAZO);
        return revisao;
    }
}
