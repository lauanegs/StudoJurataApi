package studojurata_api.ia.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.dto.SimuladoResponseDTO;
import studojurata_api.ia.dto.GerarSimuladoIARequest;
import studojurata_api.ia.dto.SimuladoGeradoIAResponseDTO;
import studojurata_api.ia.mapper.SimuladoGeradoIAMapper;
import studojurata_api.ia.repository.SimuladoGeradoIARepository;
import studojurata_api.ia.service.GeracaoSimuladoIAService;
import studojurata_api.mapper.SimuladoMapper;

import java.util.List;

/**
 * Aciona a geração automática de um simulado de reforço via IA (item 1.4).
 * O simulado retornado nasce em RASCUNHO — só é liberado ao aluno após o
 * professor revisar/aprovar as questões (endpoints já existentes em
 * /questoes/pendentes, /questoes/{id}/aprovar) e chamar
 * /simulados/{id}/lancar.
 */
@RestController
@RequestMapping("/ia/geracao")
@RequiredArgsConstructor
public class GeracaoIAController {

    private final GeracaoSimuladoIAService service;
    private final SimuladoMapper simuladoMapper;
    private final SimuladoGeradoIARepository simuladoGeradoIARepository;
    private final SimuladoGeradoIAMapper simuladoGeradoIAMapper;

    @PostMapping("/simulado")
    public SimuladoResponseDTO gerarSimulado(@Valid @RequestBody GerarSimuladoIARequest request) {
        return simuladoMapper.toResponseDTO(service.gerarParaAluno(
                request.getAlunoId(),
                request.getConteudoPlanoId(),
                request.getQuantidadeQuestoes(),
                request.getNivelDificuldade(),
                request.getMotivos()));
    }

    /**
     * Vínculo aluno/conteúdo/motivo de cada simulado já gerado pela IA — a
     * tela de aprovação do professor (front) já lista os simulados com
     * questões pendentes e só precisa juntar este dado extra por simuladoId.
     * Simulados sem vínculo aqui (criados manualmente, ou anteriores a esta
     * entidade) simplesmente não aparecem nesta lista.
     */
    @GetMapping("/simulado")
    public List<SimuladoGeradoIAResponseDTO> listarVinculos() {
        return simuladoGeradoIARepository.findAll().stream().map(simuladoGeradoIAMapper::toResponseDTO).toList();
    }
}
