package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.TurmaDisciplina;
import studojurata_api.repository.TurmaDisciplinaRepository;

import java.util.List;

@RestController
@RequestMapping("/turma-disciplina")
@RequiredArgsConstructor
public class TurmaDisciplinaController {

    private final TurmaDisciplinaRepository repository;

    @GetMapping public List<TurmaDisciplina> listar(){ return repository.findAll();}
    @GetMapping("/{id}") public TurmaDisciplina buscar(@PathVariable Long id){ return repository.findById(id).orElseThrow();}
    @PostMapping public TurmaDisciplina salvar(@RequestBody TurmaDisciplina o){ return repository.save(o);}
    @PutMapping("/{id}") public TurmaDisciplina atualizar(@PathVariable Long id,@RequestBody TurmaDisciplina o){ o.setId(id); return repository.save(o);}
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ repository.deleteById(id);}
}