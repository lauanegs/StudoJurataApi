package studojurata_api.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Único campo editável de um simulado já PUBLICADO; a edição geral
 * (SimuladoService.atualizar) só é permitida em RASCUNHO.
 */
@Getter
@Setter
public class EstenderDisponibilidadeRequest {

    private LocalDateTime dataFim;
}
