package studojurata_api.job;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.Simulado;
import studojurata_api.model.enums.StatusSimulado;
import studojurata_api.repository.SimuladoRepository;
import studojurata_api.service.SimuladoService;

import java.time.LocalDateTime;

/**
 * Publica o simulado na data de início e o encerra na data final declaradas na
 * criação, sem ação do professor. Cada simulado roda em transação e captura de
 * erro próprias: um que não está pronto (sem questões, questões pendentes de
 * revisão, destinatários indefinidos) não impede os demais.
 */
@Component
@RequiredArgsConstructor
public class CicloAutomaticoSimuladoJob {

    private static final Logger log = LoggerFactory.getLogger(CicloAutomaticoSimuladoJob.class);

    private final SimuladoRepository simuladoRepository;
    private final SimuladoService simuladoService;

    @Scheduled(fixedDelayString = "${studojurata.simulado.ciclo-automatico.intervalo-ms:60000}")
    public void executar() {
        LocalDateTime agora = LocalDateTime.now();
        publicarIniciados(agora);
        encerrarVencidos(agora);
    }

    void publicarIniciados(LocalDateTime agora) {
        for (Simulado simulado : simuladoRepository.findByStatusAndDataInicioLessThanEqual(StatusSimulado.RASCUNHO, agora)) {
            // Janela já vencida: publicar agora só criaria uma prova que ninguém pode fazer.
            if (simulado.getDataFim() != null && !simulado.getDataFim().isAfter(agora)) {
                continue;
            }
            try {
                simuladoService.lancarAutomaticamente(simulado.getId());
                log.info("Simulado {} publicado automaticamente (início {}).", simulado.getId(), simulado.getDataInicio());
            } catch (RegraNegocioException | RequisicaoInvalidaException naoPronto) {
                log.debug("Simulado {} ainda não pode ser publicado automaticamente: {}",
                        simulado.getId(), naoPronto.getMessage());
            } catch (RuntimeException erro) {
                log.error("Falha ao publicar automaticamente o simulado {}", simulado.getId(), erro);
            }
        }
    }

    void encerrarVencidos(LocalDateTime agora) {
        for (Simulado simulado : simuladoRepository.findByStatusAndDataFimLessThanEqual(StatusSimulado.PUBLICADO, agora)) {
            try {
                simuladoService.encerrarAutomaticamente(simulado.getId());
                log.info("Simulado {} encerrado automaticamente (fim {}).", simulado.getId(), simulado.getDataFim());
            } catch (RuntimeException erro) {
                log.error("Falha ao encerrar automaticamente o simulado {}", simulado.getId(), erro);
            }
        }
    }
}
