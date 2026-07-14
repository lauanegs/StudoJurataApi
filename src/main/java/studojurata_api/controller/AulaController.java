package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.Aula;
import studojurata_api.model.AulaConteudo;
import studojurata_api.model.Frequencia;
import studojurata_api.dto.ChamadaRequest;
import studojurata_api.service.AulaConteudoService;
import studojurata_api.service.AulaService;
import studojurata_api.service.FrequenciaService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/aulas")
@RequiredArgsConstructor
public class AulaController {

    private final AulaService service;
    private final AulaConteudoService aulaConteudoService;
    private final FrequenciaService frequenciaService;

    @GetMapping
    public List<Aula> listar() { return service.listar(); }

    @GetMapping("/{id}")
    public Aula buscar(@PathVariable Long id) { return service.buscar(id); }

    /** Aulas de um plano de aula, na ordem em que devem ser ministradas. */
    @GetMapping("/plano-aula/{planoAulaId}")
    public List<Aula> listarPorPlanoAula(@PathVariable Long planoAulaId) {
        return service.listarPorPlanoAula(planoAulaId);
    }

    @PostMapping
    public Aula salvar(@RequestBody Aula o) { return service.salvar(o); }

    @PutMapping("/{id}")
    public Aula atualizar(@PathVariable Long id, @RequestBody Aula o) { return service.atualizar(id, o); }

    /** Marca a aula como ministrada, preenchendo a data de publicação. */
    @PostMapping("/{id}/publicar")
    public Aula publicar(@PathVariable Long id,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataPublicacao) {
        return service.publicar(id, dataPublicacao);
    }

    /** Soft delete: marca a aula como INATIVA, preservando frequências e conteúdos já vinculados (ver 4.3). */
    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) { service.deletar(id); }

    // ---- Conteúdos da aula (aba "Registrar conteúdo") ----

    @GetMapping("/{id}/conteudos")
    public List<AulaConteudo> listarConteudos(@PathVariable Long id) { return aulaConteudoService.listarPorAula(id); }

    @PostMapping("/{id}/conteudos/{conteudoPlanoId}")
    public AulaConteudo vincularConteudo(@PathVariable Long id, @PathVariable Long conteudoPlanoId) {
        return aulaConteudoService.vincular(id, conteudoPlanoId);
    }

    @DeleteMapping("/{id}/conteudos/{conteudoPlanoId}")
    public void desvincularConteudo(@PathVariable Long id, @PathVariable Long conteudoPlanoId) {
        aulaConteudoService.desvincular(id, conteudoPlanoId);
    }

    // ---- Frequência/chamada (aba "Realizar chamada") ----

    @GetMapping("/{id}/frequencias")
    public List<Frequencia> listarFrequencias(@PathVariable Long id) { return frequenciaService.listarPorAula(id); }

    /** Lança a chamada completa da aula de uma só vez (um item por aluno). */
    @PostMapping("/{id}/frequencias/chamada")
    public List<Frequencia> registrarChamada(@PathVariable Long id, @RequestBody ChamadaRequest request) {
        return frequenciaService.registrarChamada(id, request);
    }
}
