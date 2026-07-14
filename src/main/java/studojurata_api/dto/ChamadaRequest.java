package studojurata_api.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Corpo de requisição para a tela "Realizar chamada": lista de presenças
 * lançadas de uma só vez para todos os alunos ativos de uma Aula.
 */
@Getter
@Setter
public class ChamadaRequest {

    private List<Item> alunos;

    @Getter
    @Setter
    public static class Item {
        private Long alunoId;
        private Boolean presente;
        private String justificativa;
    }
}
