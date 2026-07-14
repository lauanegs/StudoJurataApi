package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.AulaConteudo;
import studojurata_api.service.AulaConteudoService;

import java.util.List;

/**
 * CRUD genérico de vínculos Aula-Conteúdo. Para o fluxo principal da tela
 * "Registrar conteúdo" (com validação de que o conteúdo pertence ao plano
 * de ensino da aula), prefira os endpoints em /aulas/{id}/conteudos.
 */
@RestController
@RequestMapping("/aula-conteudo")
@RequiredArgsConstructor
public class AulaConteudoController {

    private final AulaConteudoService service;

    @GetMapping
    public List<AulaConteudo> listar() { return service.listar(); }

    @GetMapping("/{id}")
    public AulaConteudo buscar(@PathVariable Long id) { return service.buscar(id); }

    @GetMapping("/aula/{aulaId}")
    public List<AulaConteudo> listarPorAula(@PathVariable Long aulaId) { return service.listarPorAula(aulaId); }

    @PostMapping
    public AulaConteudo salvar(@RequestBody AulaConteudo o) {
        return service.vincular(o.getAula().getId(), o.getConteudoPlano().getId());
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) { service.deletar(id); }
}
