package studojurata_api.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Corpo de requisição para lançar um simulado (ver item 1.3 da Análise
 * Crítica). alunoIds só é relevante quando o Simulado tem
 * tipoDestinacao = ESPECIFICO; quando TODOS, é ignorado e os elegíveis são
 * derivados da matrícula ativa da turma.
 */
@Getter
@Setter
public class LancarSimuladoRequest {

    private List<Long> alunoIds;
}
