package studojurata_api.ia.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import studojurata_api.ia.model.HistoricoGeracaoIA;
import studojurata_api.ia.repository.HistoricoGeracaoIARepository;

import java.util.List;

/**
 * Consulta ao histórico de geração via IA (item "histórico de geração" desta
 * etapa) — auditoria de qualidade e diagnóstico de indisponibilidade/uso de
 * fallback (itens 3.3 e 7.2).
 */
@RestController
@RequestMapping("/ia/historico-geracao")
@RequiredArgsConstructor
public class HistoricoGeracaoIAController {

    private final HistoricoGeracaoIARepository repository;

    @GetMapping
    public List<HistoricoGeracaoIA> listar() {
        return repository.findAllByOrderByDataGeracaoDesc();
    }

    @GetMapping("/conteudo/{conteudoPlanoId}")
    public List<HistoricoGeracaoIA> listarPorConteudo(@PathVariable Long conteudoPlanoId) {
        return repository.findByConteudoPlanoIdOrderByDataGeracaoDesc(conteudoPlanoId);
    }

    @GetMapping("/simulado/{simuladoId}")
    public List<HistoricoGeracaoIA> listarPorSimulado(@PathVariable Long simuladoId) {
        return repository.findBySimuladoIdOrderByDataGeracaoDesc(simuladoId);
    }
}
