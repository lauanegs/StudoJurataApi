package studojurata_api.controller.institucional;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.institucional.Escola;
import studojurata_api.service.institucional.EscolaService;

import java.util.List;

@RestController
@RequestMapping("/escolas")
@RequiredArgsConstructor
public class EscolaController {

    private final EscolaService service;

    @GetMapping public List<Escola> listar(){ return service.listar(); }
}
