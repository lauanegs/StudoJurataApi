package studojurata_api.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Geração em lote de aulas pra um plano de aula (item pedido pelo usuário:
 * "no início do curso ele já faz a geração ali e vai manipulando depois",
 * em vez de cadastrar uma aula de cada vez em AulaFormulario). Segue os
 * horários já cadastrados na turma (HorarioTurma) — sem eles, não há como
 * gerar (ver AulaService.gerarLote).
 */
@Getter
@Setter
public class GerarAulasLoteRequest {
    private Integer quantidade;
    private LocalDate dataInicio;
    /** Prefixo do título de cada aula gerada — vira "{tituloBase} {ordem}". Default "Aula" quando vazio. */
    private String tituloBase;
}
