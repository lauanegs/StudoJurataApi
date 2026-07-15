package studojurata_api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlternativaRequestDTO {
    private Long questaoId;
    private String texto;
    private Boolean correta;
    private Integer ordem;
}
