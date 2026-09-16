package studojurata_api.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Corpo de requisição para estender a disponibilidade de um simulado já
 * PUBLICADO (item pedido pelo usuário: "disponibilizar por mais tempo") —
 * único campo editável depois do lançamento, diferente da edição geral
 * (SimuladoService.atualizar), que continua travada fora do RASCUNHO.
 */
@Getter
@Setter
public class EstenderDisponibilidadeRequest {

    private LocalDateTime dataFim;
}
