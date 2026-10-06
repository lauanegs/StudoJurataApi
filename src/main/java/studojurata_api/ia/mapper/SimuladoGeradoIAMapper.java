package studojurata_api.ia.mapper;

import org.springframework.stereotype.Component;
import studojurata_api.ia.dto.SimuladoGeradoIAResponse;
import studojurata_api.ia.model.SimuladoGeradoIA;

@Component
public class SimuladoGeradoIAMapper {

    public SimuladoGeradoIAResponse toResponse(SimuladoGeradoIA vinculo) {
        if (vinculo == null) return null;
        SimuladoGeradoIAResponse dto = new SimuladoGeradoIAResponse();
        dto.setSimuladoId(vinculo.getSimulado() != null ? vinculo.getSimulado().getId() : null);
        dto.setAlunoId(vinculo.getAluno() != null ? vinculo.getAluno().getId() : null);
        dto.setConteudoPlanoId(vinculo.getConteudoPlano() != null ? vinculo.getConteudoPlano().getId() : null);
        dto.setConteudoTitulo(vinculo.getConteudoPlano() != null ? vinculo.getConteudoPlano().getTitulo() : null);
        dto.setMotivos(vinculo.getMotivos());
        dto.setPrazoLancamento(vinculo.getPrazoLancamento());
        return dto;
    }
}
