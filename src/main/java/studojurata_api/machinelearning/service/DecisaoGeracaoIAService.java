package studojurata_api.machinelearning.service;

import java.util.Set;

import org.springframework.stereotype.Service;

import studojurata_api.ia.model.enums.MotivoRecomendacao;
import studojurata_api.machinelearning.model.enums.DecisaoGeracao;

/** Decide se as questões do simulado individual saem do banco ou precisam ser geradas pela IA */
@Service
public class DecisaoGeracaoIAService {
    /** @param simuladoIndividual a IA só pode ser acionada para simulado individual */
    public DecisaoGeracao decidir(boolean simuladoIndividual, Set<MotivoRecomendacao> motivos, boolean bancoSuficiente) {
        if (!simuladoIndividual) {
            return DecisaoGeracao.REUTILIZAR_BANCO;
        }
        if (!temGatilhoPedagogico(motivos)) {
            return DecisaoGeracao.REUTILIZAR_BANCO;
        }
        return bancoSuficiente ? DecisaoGeracao.REUTILIZAR_BANCO : DecisaoGeracao.GERAR_POR_IA;
    }

    private boolean temGatilhoPedagogico(Set<MotivoRecomendacao> motivos) {
        return motivos != null
                && (motivos.contains(MotivoRecomendacao.REPETICAO_ESPACADA)
                        || motivos.contains(MotivoRecomendacao.BAIXO_APROVEITAMENTO));
    }
}
