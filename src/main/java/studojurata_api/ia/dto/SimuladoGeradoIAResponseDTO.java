package studojurata_api.ia.dto;

import lombok.Getter;
import lombok.Setter;
import studojurata_api.ia.model.enums.MotivoRecomendacao;

import java.time.LocalDate;
import java.util.Set;

/**
 * Vínculo aluno/conteúdo/motivo de um simulado gerado automaticamente pela
 * IA — consumido pela tela de aprovação de simulados do front, que já lista
 * os simulados com questões pendentes e só precisa juntar este dado extra
 * por simuladoId (GET /ia/geracao/simulado).
 */
@Getter
@Setter
public class SimuladoGeradoIAResponseDTO {
    private Long simuladoId;
    private Long alunoId;
    private Long conteudoPlanoId;
    private String conteudoTitulo;
    private Set<MotivoRecomendacao> motivos;
    /** Prazo pra lançar este simulado — ver SimuladoGeradoIA.prazoLancamento. */
    private LocalDate prazoLancamento;
}
