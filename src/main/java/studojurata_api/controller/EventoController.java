package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Evento;
import studojurata_api.service.EventoService;

import java.util.List;

/** Restrito a ADMINISTRADOR para escrita (ver SecurityConfig); leitura liberada a qualquer usuário autenticado. */
@RestController
@RequestMapping("/eventos")
@RequiredArgsConstructor
public class EventoController {

    private final EventoService service;

    @GetMapping public List<Evento> listar(){ return service.listar(); }
    @GetMapping("/pendentes") public List<Evento> listarPendentes(){ return service.listarPendentes(); }
    @GetMapping("/concluidos") public List<Evento> listarConcluidos(){ return service.listarConcluidos(); }
    @GetMapping("/{id}") public Evento buscar(@PathVariable Long id){ return service.buscar(id); }
    @PostMapping public Evento salvar(@RequestBody Evento o){ return service.salvar(o); }
    @PutMapping("/{id}") public Evento atualizar(@PathVariable Long id, @RequestBody Evento o){ return service.atualizar(id, o); }
    @DeleteMapping("/{id}") public void deletar(@PathVariable Long id){ service.deletar(id); }
}
