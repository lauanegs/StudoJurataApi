package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studojurata_api.model.AuditLog;
import studojurata_api.service.AuditLogService;

import java.util.List;

/** Consulta de auditoria (item 10.4) — restrito a ADMINISTRADOR via SecurityConfig ("/audit-log/**"). */
@RestController
@RequestMapping("/audit-log")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService service;

    @GetMapping
    public List<AuditLog> historico(@RequestParam String entidade, @RequestParam Long entidadeId) {
        return service.historicoDaEntidade(entidade, entidadeId);
    }
}
