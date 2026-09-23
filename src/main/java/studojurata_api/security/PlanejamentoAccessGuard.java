package studojurata_api.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import studojurata_api.model.enums.TipoUsuario;

/**
 * Escopo do domínio de planejamento: plano de ensino, plano de aula, conteúdo do
 * plano, aula e vínculo turma-disciplina.
 *
 * <p>A pergunta é sempre a mesma — o vínculo turma-disciplina a que o recurso
 * pertence está no escopo do usuário logado? —, então ela vive aqui em vez de
 * ser reescrita em cada service (a fonte do escopo continua sendo o
 * {@link EscopoUsuario}, o mesmo usado pelas listagens).
 *
 * <p>Regras:
 * <ul>
 *   <li>ADMINISTRADOR passa em tudo;</li>
 *   <li>escrita é de PROFESSOR (no próprio escopo) e de ADMINISTRADOR — ALUNO
 *       nunca escreve planejamento;</li>
 *   <li>recurso <b>sem vínculo</b> (plano genérico ou conteúdo sem plano,
 *       preservados por D-C2) continua legível, mas só o ADMIN escreve: sem
 *       vínculo não há como provar posse, mesma cautela adotada para simulado
 *       órfão no C2.7d.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class PlanejamentoAccessGuard {

    private final UsuarioAutenticado usuarioAutenticado;
    private final EscopoUsuario escopoUsuario;
    private final EscopoProfessor escopoProfessor;

    /** Leitura: vínculo no escopo ou recurso sem vínculo (D-C2, visível a autenticados). */
    public void garantirLeitura(Long vinculoId, String mensagem) {
        if (usuarioAutenticado.ehAdministrador()) {
            return;
        }
        if (vinculoId == null || escopoUsuario.turmaDisciplinaIds().contains(vinculoId)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, mensagem);
    }

    /** Escrita: perfil de gestão e vínculo no escopo (recurso sem vínculo é do ADMIN). */
    public void garantirEscrita(Long vinculoId, String mensagem) {
        if (usuarioAutenticado.ehAdministrador()) {
            return;
        }
        exigirPerfilDeEscrita();
        if (vinculoId == null || !escopoUsuario.turmaDisciplinaIds().contains(vinculoId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, mensagem);
        }
    }

    /** Escrita recortada por turma (criação de vínculo turma-disciplina). */
    public void garantirEscritaNaTurma(Long turmaId, String mensagem) {
        if (usuarioAutenticado.ehAdministrador()) {
            return;
        }
        exigirPerfilDeEscrita();
        Long professorId = usuarioAutenticado.professorId();
        if (turmaId == null || professorId == null
                || !escopoProfessor.turmaIdsDoProfessor(professorId).contains(turmaId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, mensagem);
        }
    }

    /** 403 para quem não é professor nem administrador (ALUNO não escreve planejamento). */
    public void exigirPerfilDeEscrita() {
        var usuario = usuarioAutenticado.atual();
        if (usuario.getTipoUsuario() != TipoUsuario.PROFESSOR
                && usuario.getTipoUsuario() != TipoUsuario.ADMINISTRADOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Apenas professor ou administrador pode alterar o planejamento.");
        }
    }
}
