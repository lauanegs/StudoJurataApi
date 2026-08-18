package studojurata_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import studojurata_api.model.NotificacaoEnviada;
import studojurata_api.model.ResponsavelAluno;
import studojurata_api.model.enums.TipoNotificacao;
import studojurata_api.repository.NotificacaoEnviadaRepository;

import java.util.List;

/**
 * Item 9.8 do documento de regras: notificação a responsáveis. Só estrutura
 * + registro em banco por enquanto — sem provedor de e-mail/SMS configurado,
 * "enviada" fica sempre false. Ver gatilhos em NotaService.recalcular
 * (NOVA_NOTA) e EventoService.salvar (NOVO_EVENTO).
 */
@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private final NotificacaoEnviadaRepository repository;

    public NotificacaoEnviada registrar(ResponsavelAluno responsavelAluno, TipoNotificacao tipo, String mensagem) {
        NotificacaoEnviada notificacao = new NotificacaoEnviada();
        notificacao.setResponsavelAluno(responsavelAluno);
        notificacao.setTipo(tipo);
        notificacao.setMensagem(mensagem);
        notificacao.setEnviada(false);
        return repository.save(notificacao);
    }

    public List<NotificacaoEnviada> listar() {
        return repository.findAllByOrderByCreatedAtDesc();
    }
}
