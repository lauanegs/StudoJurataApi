package studojurata_api.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Segue os horários cadastrados na turma (HorarioTurma); sem eles não há como gerar. */
@Getter
@Setter
public class GerarAulasLoteRequest {
    private Integer quantidade;
    private LocalDate dataInicio;
    /** Prefixo do título de cada aula gerada — vira "{tituloBase} {ordem}". Default "Aula" quando vazio. */
    private String tituloBase;
}
