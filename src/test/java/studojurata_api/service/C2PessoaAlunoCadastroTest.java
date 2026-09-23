package studojurata_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import studojurata_api.exception.RequisicaoInvalidaException;
import studojurata_api.model.Aluno;
import studojurata_api.model.Pessoa;
import studojurata_api.model.enums.Sexo;
import studojurata_api.repository.AlunoRepository;
import studojurata_api.repository.AlunoTurmaRepository;
import studojurata_api.repository.PessoaRepository;
import studojurata_api.security.EscopoProfessor;
import studojurata_api.security.UsuarioAutenticado;

/**
 * Bloco A — cadastro de pessoa e de aluno.
 *
 * <p>Três regras que só existiam na tela: sexo obrigatório em todo cadastro de
 * pessoa, data de nascimento obrigatória no aluno e matrícula gerada pelo
 * servidor (o usuário não escolhe o código).
 */
@ExtendWith(MockitoExtension.class)
class C2PessoaAlunoCadastroTest {

    private static final long ALUNO_ID = 7L;
    private static final String ANO = String.valueOf(LocalDate.now().getYear());

    @Mock private PessoaRepository pessoaRepository;
    @Mock private AlunoRepository alunoRepository;
    @Mock private AlunoTurmaRepository alunoTurmaRepository;
    @Mock private EscopoProfessor escopoProfessor;

    private PessoaService pessoaService;
    private AlunoService alunoService;

    @BeforeEach
    void setUp() {
        pessoaService = new PessoaService(pessoaRepository);
        alunoService = new AlunoService(alunoRepository, alunoTurmaRepository,
                new UsuarioAutenticado(), escopoProfessor);
    }

    // --- sexo obrigatório ---------------------------------------------------

    @Test
    @DisplayName("pessoa sem sexo é recusada")
    void pessoaSemSexoEhRecusada() {
        Pessoa pessoa = pessoa("800.000.001-67");
        pessoa.setSexo(null);

        assertInvalida(() -> pessoaService.salvar(pessoa), "Sexo");

        verify(pessoaRepository, never()).save(any(Pessoa.class));
    }

    @Test
    @DisplayName("pessoa com sexo é salva")
    void pessoaComSexoEhSalva() {
        given(pessoaRepository.save(any(Pessoa.class))).willAnswer(chamada -> chamada.getArgument(0));

        assertThat(pessoaService.salvar(pessoa("800.000.001-67")).getSexo()).isEqualTo(Sexo.FEMININO);

        verify(pessoaRepository).save(any(Pessoa.class));
    }

    // --- data de nascimento do aluno ---------------------------------------

    @Test
    @DisplayName("aluno sem data de nascimento é recusado")
    void alunoSemDataNascimentoEhRecusado() {
        Aluno aluno = new Aluno();
        aluno.setPessoa(pessoa("800.000.001-67"));

        assertInvalida(() -> alunoService.salvar(aluno), "Data de nascimento");

        verify(alunoRepository, never()).save(any(Aluno.class));
    }

    // --- matrícula gerada ---------------------------------------------------

    @Test
    @DisplayName("matrícula é gerada pelo servidor, ignorando o que o cliente mandou")
    void matriculaEhGeradaPeloServidor() {
        given(alunoRepository.buscarUltimaMatriculaComPrefixo(ANO)).willReturn(Optional.empty());
        given(alunoRepository.existsByMatricula(ANO + "0001")).willReturn(false);
        given(alunoRepository.save(any(Aluno.class))).willAnswer(chamada -> chamada.getArgument(0));

        Aluno aluno = alunoCom(true);
        aluno.setMatricula("CODIGO-DO-CLIENTE");

        assertThat(alunoService.salvar(aluno).getMatricula()).isEqualTo(ANO + "0001");
    }

    @Test
    @DisplayName("matrícula continua a sequência do ano")
    void matriculaSegueASequencia() {
        given(alunoRepository.buscarUltimaMatriculaComPrefixo(ANO)).willReturn(Optional.of(ANO + "0007"));
        given(alunoRepository.existsByMatricula(ANO + "0008")).willReturn(false);
        given(alunoRepository.save(any(Aluno.class))).willAnswer(chamada -> chamada.getArgument(0));

        assertThat(alunoService.salvar(alunoCom(true)).getMatricula()).isEqualTo(ANO + "0008");
    }

    @Test
    @DisplayName("matrícula já usada é pulada")
    void matriculaColididaEhPulada() {
        given(alunoRepository.buscarUltimaMatriculaComPrefixo(ANO)).willReturn(Optional.of(ANO + "0007"));
        given(alunoRepository.existsByMatricula(ANO + "0008")).willReturn(true);
        given(alunoRepository.existsByMatricula(ANO + "0009")).willReturn(false);
        given(alunoRepository.save(any(Aluno.class))).willAnswer(chamada -> chamada.getArgument(0));

        assertThat(alunoService.salvar(alunoCom(true)).getMatricula()).isEqualTo(ANO + "0009");
    }

    @Test
    @DisplayName("matrícula fora do padrão reinicia a sequência sem repetir código")
    void matriculaForaDoPadraoNaoRepete() {
        given(alunoRepository.buscarUltimaMatriculaComPrefixo(ANO)).willReturn(Optional.of(ANO + "AAA"));
        given(alunoRepository.existsByMatricula(ANO + "0001")).willReturn(true);
        given(alunoRepository.existsByMatricula(ANO + "0002")).willReturn(false);
        given(alunoRepository.save(any(Aluno.class))).willAnswer(chamada -> chamada.getArgument(0));

        assertThat(alunoService.salvar(alunoCom(true)).getMatricula()).isEqualTo(ANO + "0002");
    }

    @Test
    @DisplayName("edição preserva a matrícula do cadastro")
    void edicaoPreservaMatricula() {
        Aluno existente = new Aluno();
        existente.setId(ALUNO_ID);
        existente.setMatricula(ANO + "0003");
        given(alunoRepository.findById(ALUNO_ID)).willReturn(Optional.of(existente));
        given(alunoRepository.save(any(Aluno.class))).willAnswer(chamada -> chamada.getArgument(0));

        Aluno alteracao = alunoCom(true);
        alteracao.setMatricula("OUTRA");

        assertThat(alunoService.atualizar(ALUNO_ID, alteracao).getMatricula()).isEqualTo(ANO + "0003");
    }

    // --- apoio --------------------------------------------------------------

    private static Pessoa pessoa(String cpf) {
        Pessoa pessoa = new Pessoa();
        pessoa.setNome("Rafael Torres Mendes");
        pessoa.setCpf(cpf);
        pessoa.setSexo(Sexo.FEMININO);
        return pessoa;
    }

    private static Aluno alunoCom(boolean comDataDeNascimento) {
        Pessoa pessoa = pessoa("800.000.001-67");
        if (comDataDeNascimento) {
            pessoa.setDataNascimento(LocalDate.of(2015, 5, 20));
        }

        Aluno aluno = new Aluno();
        aluno.setPessoa(pessoa);
        return aluno;
    }

    private static void assertInvalida(Runnable acao, String trecho) {
        try {
            acao.run();
            fail("esperava 400");
        } catch (RequisicaoInvalidaException esperada) {
            assertThat(esperada.getMessage()).contains(trecho);
        }
    }
}
