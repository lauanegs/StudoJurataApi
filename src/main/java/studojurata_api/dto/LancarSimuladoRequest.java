package studojurata_api.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * alunoIds só vale para tipoDestinacao = ESPECIFICO; com TODOS, os elegíveis
 * vêm das matrículas ativas da turma.
 */
@Getter
@Setter
public class LancarSimuladoRequest {

    private List<Long> alunoIds;
}
