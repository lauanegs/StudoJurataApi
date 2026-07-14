package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.ResponsavelAluno;
import studojurata_api.repository.ResponsavelAlunoRepository;

import java.util.List;

@RestController
@RequestMapping("/responsavel-aluno")
@RequiredArgsConstructor
public class ResponsavelAlunoController {

    private final ResponsavelAlunoRepository repository;

    @GetMapping public List<ResponsavelAluno> listar(){ return repository.findAll();}
    @GetMapping("/{id}") public ResponsavelAluno buscar(@PathVariable Long id){ return repository.findById(id).orElseThrow();}
    @GetMapping("/por-aluno/{alunoId}") public List<ResponsavelAluno> porAluno(@PathVariable Long alunoId){ return repository.findByAlunoId(alunoId);}
    @GetMapping("/por-responsavel/{responsavelId}") public List<ResponsavelAluno> porResponsavel(@PathVariable Long responsavelId){ return repository.findByResponsavelId(responsavelId);}
    @PostMapping public ResponsavelAluno salvar(@RequestBody ResponsavelAluno o){ return repository.save(o);}
    @PutMapping("/{id}") public ResponsavelAluno atualizar(@PathVariable Long id,@RequestBody ResponsavelAluno o){ o.setId(id); return repository.save(o);}
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ repository.deleteById(id);}
}
