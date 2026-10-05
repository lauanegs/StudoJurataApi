package studojurata_api.controller.pessoa;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.pessoa.Pessoa;
import studojurata_api.service.pessoa.PessoaService;

import java.util.List;

@RestController
@RequestMapping("/pessoas")
@RequiredArgsConstructor
public class PessoaController {

    private final PessoaService service;

    @GetMapping public List<Pessoa> listar(){ return service.listar(); }
    @PostMapping public Pessoa salvar(@RequestBody Pessoa o){ return service.salvar(o); }
    @PutMapping("/{id}") public Pessoa atualizar(@PathVariable Long id,@RequestBody Pessoa o){ return service.atualizar(id, o); }
}
