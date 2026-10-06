package studojurata_api.dto.planejamento;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

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
