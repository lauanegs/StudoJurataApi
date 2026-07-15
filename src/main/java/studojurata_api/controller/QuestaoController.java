package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.dto.QuestaoRequestDTO;
import studojurata_api.dto.QuestaoResponseDTO;
import studojurata_api.mapper.QuestaoMapper;
import studojurata_api.service.QuestaoService;

import java.util.List;

@RestController
@RequestMapping("/questoes")
@RequiredArgsConstructor
public class QuestaoController {

    private final QuestaoService service;
    private final QuestaoMapper mapper;

    @GetMapping
    public List<QuestaoResponseDTO> listar() {
        return service.listar().stream().map(mapper::toResponseDTO).toList();
    }

    @GetMapping("/{id}")
    public QuestaoResponseDTO buscar(@PathVariable Long id) {
        return mapper.toResponseDTO(service.buscar(id));
    }

    @PostMapping
    public QuestaoResponseDTO salvar(@RequestBody QuestaoRequestDTO dto) {
        return mapper.toResponseDTO(service.salvar(mapper.toEntity(dto)));
    }

    @PutMapping("/{id}")
    public QuestaoResponseDTO atualizar(@PathVariable Long id, @RequestBody QuestaoRequestDTO dto) {
        return mapper.toResponseDTO(service.atualizar(id, mapper.toEntity(dto)));
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) { service.deletar(id); }

    /** Fila da tela de Revisão do professor (itens 1.4/7.3). */
    @GetMapping("/pendentes")
    public List<QuestaoResponseDTO> listarPendentes() {
        return service.listarPendentes().stream().map(mapper::toResponseDTO).toList();
    }

    @PostMapping("/{id}/aprovar")
    public QuestaoResponseDTO aprovar(@PathVariable Long id) {
        return mapper.toResponseDTO(service.aprovar(id));
    }

    @PostMapping("/{id}/rejeitar")
    public QuestaoResponseDTO rejeitar(@PathVariable Long id) {
        return mapper.toResponseDTO(service.rejeitar(id));
    }
}
