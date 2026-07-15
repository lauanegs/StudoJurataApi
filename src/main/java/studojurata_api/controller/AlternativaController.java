package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.dto.AlternativaRequestDTO;
import studojurata_api.dto.AlternativaResponseDTO;
import studojurata_api.mapper.AlternativaMapper;
import studojurata_api.service.AlternativaService;

import java.util.List;

@RestController
@RequestMapping("/alternativas")
@RequiredArgsConstructor
public class AlternativaController {

    private final AlternativaService service;
    private final AlternativaMapper mapper;

    @GetMapping
    public List<AlternativaResponseDTO> listar() {
        return service.listar().stream().map(mapper::toResponseDTO).toList();
    }

    @GetMapping("/{id}")
    public AlternativaResponseDTO buscar(@PathVariable Long id) {
        return mapper.toResponseDTO(service.buscar(id));
    }

    @PostMapping
    public AlternativaResponseDTO salvar(@RequestBody AlternativaRequestDTO dto) {
        return mapper.toResponseDTO(service.salvar(mapper.toEntity(dto)));
    }

    @PutMapping("/{id}")
    public AlternativaResponseDTO atualizar(@PathVariable Long id, @RequestBody AlternativaRequestDTO dto) {
        return mapper.toResponseDTO(service.atualizar(id, mapper.toEntity(dto)));
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) { service.deletar(id); }
}
