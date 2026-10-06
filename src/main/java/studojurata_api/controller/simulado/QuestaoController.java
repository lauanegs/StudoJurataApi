package studojurata_api.controller.simulado;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.dto.simulado.QuestaoRequest;
import studojurata_api.dto.simulado.QuestaoResponse;
import studojurata_api.mapper.simulado.QuestaoMapper;
import studojurata_api.model.simulado.QuestaoConteudo;
import studojurata_api.service.simulado.QuestaoConteudoService;
import studojurata_api.service.simulado.QuestaoService;

import java.util.List;

@RestController
@RequestMapping("/questoes")
@RequiredArgsConstructor
public class QuestaoController {

    private final QuestaoService service;
    private final QuestaoMapper mapper;
    private final QuestaoConteudoService questaoConteudoService;

    @GetMapping
    public List<QuestaoResponse> listar() {
        return service.listar().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public QuestaoResponse buscar(@PathVariable Long id) {
        return mapper.toResponse(service.buscar(id));
    }

    @PostMapping
    public QuestaoResponse salvar(@Valid @RequestBody QuestaoRequest dto) {
        return mapper.toResponse(service.salvar(mapper.toEntity(dto)));
    }

    @PutMapping("/{id}")
    public QuestaoResponse atualizar(@PathVariable Long id, @Valid @RequestBody QuestaoRequest dto) {
        return mapper.toResponse(service.atualizar(id, mapper.toEntity(dto)));
    }

    @GetMapping("/pendentes")
    public List<QuestaoResponse> listarPendentes() {
        return service.listarPendentes().stream().map(mapper::toResponse).toList();
    }

    @PostMapping("/{id}/aprovar")
    public QuestaoResponse aprovar(@PathVariable Long id) {
        return mapper.toResponse(service.aprovar(id));
    }

    @PostMapping("/{id}/rejeitar")
    public QuestaoResponse rejeitar(@PathVariable Long id) {
        return mapper.toResponse(service.rejeitar(id));
    }

    @GetMapping("/{id}/conteudos")
    public List<QuestaoConteudo> listarConteudos(@PathVariable Long id) {
        // Reaproveita o escopo de leitura da questao (404 se nao existir,
        // 403 fora do escopo) em vez de duplicar a regra aqui.
        service.buscar(id);
        return questaoConteudoService.listarPorQuestao(id);
    }

    @PostMapping("/{id}/conteudos/{conteudoPlanoId}")
    public QuestaoConteudo vincularConteudo(@PathVariable Long id, @PathVariable Long conteudoPlanoId) {
        return questaoConteudoService.vincular(id, conteudoPlanoId);
    }

    @DeleteMapping("/{id}/conteudos/{conteudoPlanoId}")
    public void desvincularConteudo(@PathVariable Long id, @PathVariable Long conteudoPlanoId) {
        questaoConteudoService.desvincular(id, conteudoPlanoId);
    }
}
