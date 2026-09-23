package studojurata_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.Aula;
import studojurata_api.repository.AulaRepository;
import studojurata_api.repository.HorarioTurmaRepository;
import studojurata_api.repository.PlanoAulaRepository;
import studojurata_api.repository.PlanoEnsinoRepository;
import studojurata_api.security.EscopoUsuario;
import studojurata_api.security.PlanejamentoAccessGuard;
import studojurata_api.security.UsuarioAutenticado;

/**
 * Publicação de aula (Fase 1 do M8): "ministrada" é a data em que a aula
 * realmente aconteceu, então data futura é recusada — é ela que libera conteúdo
 * para o reforço por IA e alimenta as estatísticas do plano.
 *
 * <p>O escopo da publicação é exercitado em {@code C2PlanejamentoEscopoTest};
 * aqui o alvo é a regra de data.
 */
@ExtendWith(MockitoExtension.class)
class C2AulaPublicacaoTest {

    private static final long AULA_ID = 300L;

    @Mock private AulaRepository repository;
    @Mock private PlanoAulaRepository planoAulaRepository;
    @Mock private PlanoEnsinoRepository planoEnsinoRepository;
    @Mock private HorarioTurmaRepository horarioTurmaRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private EscopoUsuario escopoUsuario;
    @Mock private PlanejamentoAccessGuard planejamentoAccessGuard;

    private AulaService service;

    @Test
    @DisplayName("publicação com data futura é recusada")
    void publicacaoComDataFuturaEhRecusada() {
        dadoAulaExistente();

        try {
            service().publicar(AULA_ID, LocalDate.now().plusDays(1));
            fail("esperava recusa de data futura");
        } catch (RequisicaoInvalidaException esperada) {
            assertThat(esperada.getMessage()).contains("futura");
        }

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("publicação com a data de hoje é aceita")
    void publicacaoComDataDeHojeEhAceita() {
        dadoAulaExistente();
        given(repository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Aula publicada = service().publicar(AULA_ID, LocalDate.now());

        assertThat(publicada.getDataPublicacao()).isEqualTo(LocalDate.now());
        verify(repository).save(any());
    }

    @Test
    @DisplayName("publicação sem data continua registrando hoje")
    void publicacaoSemDataRegistraHoje() {
        dadoAulaExistente();
        given(repository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        service().publicar(AULA_ID, null);

        ArgumentCaptor<Aula> salva = ArgumentCaptor.forClass(Aula.class);
        verify(repository).save(salva.capture());
        assertThat(salva.getValue().getDataPublicacao()).isEqualTo(LocalDate.now());
    }

    private AulaService service() {
        return new AulaService(repository, planoAulaRepository, planoEnsinoRepository, horarioTurmaRepository,
                auditLogService, new UsuarioAutenticado(), escopoUsuario, planejamentoAccessGuard);
    }

    private void dadoAulaExistente() {
        given(repository.findById(AULA_ID)).willReturn(Optional.of(new Aula()));
    }
}
