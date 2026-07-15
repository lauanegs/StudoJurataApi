package studojurata_api.ia.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import studojurata_api.ia.dto.HistoricoGeracaoIAResponseDTO;
import studojurata_api.ia.mapper.HistoricoGeracaoIAMapper;
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
    private final HistoricoGeracaoIAMapper mapper;

    @GetMapping
    public List<HistoricoGeracaoIAResponseDTO> listar() {
        return repository.findAllByOrderByDataGeracaoDesc().stream().map(mapper::toResponseDTO).toList();
    }

    @GetMapping("/conteudo/{conteudoPlanoId}")
    public List<HistoricoGeracaoIAResponseDTO> listarPorConteudo(@PathVariable Long conteudoPlanoId) {
        return repository.findByConteudoPlanoIdOrderByDataGeracaoDesc(conteudoPlanoId).stream().map(mapper::toResponseDTO).toList();
    }

    @GetMapping("/simulado/{simuladoId}")
    public List<HistoricoGeracaoIAResponseDTO> listarPorSimulado(@PathVariable Long simuladoId) {
        return repository.findBySimuladoIdOrderByDataGeracaoDesc(simuladoId).stream().map(mapper::toResponseDTO).toList();
    }
}
