package studojurata_api.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import studojurata_api.exception.RegraNegocioException;
import studojurata_api.model.Simulado;
import studojurata_api.model.enums.StatusSimulado;
import studojurata_api.repository.SimuladoRepository;
import studojurata_api.service.SimuladoService;

@ExtendWith(MockitoExtension.class)
class CicloAutomaticoSimuladoJobTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 5, 12, 0);

    @Mock private SimuladoRepository simuladoRepository;
    @Mock private SimuladoService simuladoService;
    @InjectMocks private CicloAutomaticoSimuladoJob job;

    @Test
    @DisplayName("publica o rascunho cuja data de início já chegou")
    void publicaRascunhoIniciado() {
        given(simuladoRepository.findByStatusAndDataInicioLessThanEqual(StatusSimulado.RASCUNHO, AGORA))
                .willReturn(List.of(simulado(1L, AGORA.minusHours(1), AGORA.plusDays(1))));

        job.publicarIniciados(AGORA);

        verify(simuladoService).lancarAutomaticamente(1L);
    }

    @Test
    @DisplayName("não publica rascunho cuja janela já terminou")
    void naoPublicaJanelaVencida() {
        given(simuladoRepository.findByStatusAndDataInicioLessThanEqual(StatusSimulado.RASCUNHO, AGORA))
                .willReturn(List.of(simulado(1L, AGORA.minusDays(2), AGORA.minusDays(1))));

        job.publicarIniciados(AGORA);

        verify(simuladoService, never()).lancarAutomaticamente(any());
    }

    @Test
    @DisplayName("um simulado que ainda não está pronto não impede a publicação dos demais")
    void simuladoNaoProntoNaoBloqueiaOsDemais() {
        given(simuladoRepository.findByStatusAndDataInicioLessThanEqual(StatusSimulado.RASCUNHO, AGORA))
                .willReturn(List.of(simulado(1L, AGORA.minusHours(1), null), simulado(2L, AGORA.minusHours(1), null)));
        doThrow(new RegraNegocioException("sem questões")).when(simuladoService).lancarAutomaticamente(1L);

        job.publicarIniciados(AGORA);

        verify(simuladoService).lancarAutomaticamente(2L);
    }

    @Test
    @DisplayName("encerra o publicado cuja data final já passou")
    void encerraPublicadoVencido() {
        given(simuladoRepository.findByStatusAndDataFimLessThanEqual(StatusSimulado.PUBLICADO, AGORA))
                .willReturn(List.of(simulado(3L, AGORA.minusDays(3), AGORA.minusMinutes(1))));

        job.encerrarVencidos(AGORA);

        verify(simuladoService).encerrarAutomaticamente(3L);
    }

    private static Simulado simulado(Long id, LocalDateTime inicio, LocalDateTime fim) {
        Simulado simulado = new Simulado();
        simulado.setId(id);
        simulado.setDataInicio(inicio);
        simulado.setDataFim(fim);
        return simulado;
    }
}
