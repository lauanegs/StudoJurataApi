package studojurata_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import studojurata_api.model.NotificacaoEnviada;
import studojurata_api.service.NotificacaoService;

import java.util.List;

/** Item 9.8 — somente leitura, restrito a ADMINISTRADOR (ver SecurityConfig). */
@RestController
@RequestMapping("/notificacoes")
@RequiredArgsConstructor
public class NotificacaoController {

    private final NotificacaoService service;

    @GetMapping public List<NotificacaoEnviada> listar(){ return service.listar(); }
}
