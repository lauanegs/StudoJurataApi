package studojurata_api.controller.simulado;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.dto.simulado.AlternativaRequestDTO;
import studojurata_api.dto.simulado.AlternativaResponseDTO;
import studojurata_api.mapper.simulado.AlternativaMapper;
import studojurata_api.service.simulado.AlternativaService;

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

    @PostMapping
    public AlternativaResponseDTO salvar(@Valid @RequestBody AlternativaRequestDTO dto) {
        return mapper.toResponseDTO(service.salvar(mapper.toEntity(dto)));
    }

    @PutMapping("/{id}")
    public AlternativaResponseDTO atualizar(@PathVariable Long id, @Valid @RequestBody AlternativaRequestDTO dto) {
        return mapper.toResponseDTO(service.atualizar(id, mapper.toEntity(dto)));
    }
}
