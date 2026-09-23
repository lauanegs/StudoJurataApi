package studojurata_api.service;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import studojurata_api.model.enums.TipoUsuario;
import studojurata_api.security.EscopoProfessor;
import studojurata_api.security.UsuarioAutenticado;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.exception.RegraNegocioException;
import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.Aluno;
import studojurata_api.model.AlunoTurma;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.AlunoTurmaRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlunoService {

    private final AlunoRepository repository;
    private final AlunoTurmaRepository alunoTurmaRepository;
    private final UsuarioAutenticado usuarioAutenticado;
    private final EscopoProfessor escopoProfessor;

    /**
     * Listagem escopada: ADMINISTRADOR mantem a visao completa (Aluno/Pessoa nao
     * tem vinculo de escola no modelo); PROFESSOR ve os alunos matriculados nas
     * turmas em que leciona, em qualquer status de matricula (historico
     * preservado); ALUNO ve apenas o proprio cadastro.
     */
    public List<Aluno> listar() {
        var usuario = usuarioAutenticado.atual();

        if (usuario.getTipoUsuario() == TipoUsuario.ADMINISTRADOR) {
            return repository.findAll();
        }

        if (usuario.getTipoUsuario() == TipoUsuario.ALUNO) {
            Long alunoId = usuario.getAluno() != null ? usuario.getAluno().getId() : null;
            return alunoId == null ? List.of() : repository.findById(alunoId).map(List::of).orElseGet(List::of);
        }

        Long professorId = usuario.getProfessor() != null ? usuario.getProfessor().getId() : null;
        Set<Long> turmaIds = escopoProfessor.turmaIdsDoProfessor(professorId);
        if (turmaIds.isEmpty()) {
            return List.of();
        }

        // Uma consulta de matriculas + uma de alunos; ids distintos evitam
        // duplicidade de quem esta em mais de uma turma.
        Set<Long> alunoIds = alunoTurmaRepository.findByTurma_IdIn(turmaIds).stream()
                .map(AlunoTurma::getAluno)
                .filter(Objects::nonNull)
                .map(Aluno::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return alunoIds.isEmpty() ? List.of() : repository.findAllById(alunoIds);
    }

    public Aluno buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno " + id + " não encontrado."));
    }

    public Aluno salvar(Aluno obj) {
        exigirDataNascimento(obj);
        // Matrícula é código da escola, não do usuário: o servidor gera o
        // próximo da sequência do ano (AAAA9999) em vez de aceitar o digitado.
        obj.setMatricula(gerarMatricula());
        return repository.save(obj);
    }

    public Aluno atualizar(Long id, Aluno obj) {
        Aluno existente = buscar(id);
        exigirDataNascimento(obj);
        // A matrícula acompanha o cadastro: o cliente não a informa nem a altera.
        obj.setMatricula(existente.getMatricula());
        obj.setId(id);
        return repository.save(obj);
    }

    /**
     * Data de nascimento é obrigatória no cadastro do aluno: é ela que define a
     * exigência de responsável (menor de idade) e a idade exibida nas telas.
     */
    private void exigirDataNascimento(Aluno obj) {
        if (obj.getPessoa() == null || obj.getPessoa().getDataNascimento() == null) {
            throw new RequisicaoInvalidaException("Data de nascimento é obrigatória para o aluno.");
        }
    }

    /** Próxima matrícula livre do ano: AAAA + sequência de 4 dígitos. */
    private String gerarMatricula() {
        String prefixo = String.valueOf(LocalDate.now().getYear());
        int proximo = repository.buscarUltimaMatriculaComPrefixo(prefixo)
                .map(ultima -> sequenciaDe(ultima, prefixo))
                .orElse(0) + 1;

        String candidata;
        do {
            candidata = prefixo + String.format("%04d", proximo++);
        } while (repository.existsByMatricula(candidata));
        return candidata;
    }

    private static int sequenciaDe(String matricula, String prefixo) {
        try {
            return Integer.parseInt(matricula.substring(prefixo.length()));
        } catch (RuntimeException erro) {
            // Matrícula antiga fora do padrão: reinicia a sequência; o laço de
            // colisão acima garante que nenhum código existente seja repetido.
            return 0;
        }
    }

    /**
     * Soft-delete pela Pessoa, já que Aluno não tem status próprio. Recusa
     * aluno com qualquer matrícula: a exclusão serve só para descartar
     * cadastro feito por engano.
     */
    @Transactional
    public void deletar(Long id) {
        Aluno aluno = buscar(id);
        if (alunoTurmaRepository.existsByAluno_Id(id)) {
            throw new RegraNegocioException(
                    "Este aluno já teve matrícula em turma e não pode ser inativado.");
        }
        if (aluno.getPessoa() != null) {
            aluno.getPessoa().setStatus(StatusAtivoInativo.INATIVO);
        }
    }

    @Transactional
    public Aluno ativar(Long id) {
        Aluno aluno = buscar(id);
        if (aluno.getPessoa() != null) {
            aluno.getPessoa().setStatus(StatusAtivoInativo.ATIVO);
        }
        return aluno;
    }
}
