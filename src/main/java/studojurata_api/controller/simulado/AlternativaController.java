package studojurata_api.controller.simulado;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.dto.simulado.AlternativaRequest;
import studojurata_api.dto.simulado.AlternativaResponse;
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
    public List<AlternativaResponse> listar() {
        return service.listar().stream().map(mapper::toResponse).toList();
    }

    @PostMapping
    public AlternativaResponse salvar(@Valid @RequestBody AlternativaRequest dto) {
        return mapper.toResponse(service.salvar(mapper.toEntity(dto)));
    }

    @PutMapping("/{id}")
    public AlternativaResponse atualizar(@PathVariable Long id, @Valid @RequestBody AlternativaRequest dto) {
        return mapper.toResponse(service.atualizar(id, mapper.toEntity(dto)));
    }
}
