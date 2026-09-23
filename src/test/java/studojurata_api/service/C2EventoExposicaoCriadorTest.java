package studojurata_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import studojurata_api.model.Endereco;
import studojurata_api.model.Escola;
import studojurata_api.model.Evento;
import studojurata_api.model.Pessoa;
import studojurata_api.model.Usuario;
import studojurata_api.model.enums.StatusAtivoInativo;
import studojurata_api.model.enums.TipoUsuario;
import studojurata_api.repository.EventoRepository;

/**
 * Eventos sao lidos por qualquer usuario autenticado (a Home de todos os perfis
 * mostra o calendario). A entidade aponta para o Usuario que criou o evento, e o
 * Usuario carrega a Pessoa (CPF, contato, endereco) - dado que nao pertence a
 * quem apenas consulta o calendario.
 */
@ExtendWith(MockitoExtension.class)
class C2EventoExposicaoCriadorTest {

    @Mock private EventoRepository repository;

    private EventoService service;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        service = new EventoService(repository);
    }

    @Test
    @DisplayName("a resposta de evento nao expoe quem criou nem os dados pessoais do usuario")
    void eventoNaoExpoeCriador() throws Exception {
        Evento evento = new Evento();
        evento.setId(1L);
        evento.setTitulo("Feira de ciencia");
        evento.setDescricao("Apresentacao dos projetos");
        evento.setDataHorario(LocalDateTime.of(2026, 9, 22, 19, 0));
        evento.setConcluido(false);
        evento.setCriadoPor(usuarioComDadosPessoais());

        String json = objectMapper.writeValueAsString(evento);

        assertThat(json)
                .contains("Feira de ciencia")
                .doesNotContain("criadoPor")
                .doesNotContain("123.456.789-00")
                .doesNotContain("Rua das Flores");
    }

    @Test
    @DisplayName("editar um evento preserva quem o criou quando o corpo nao traz o campo")
    void edicaoPreservaCriador() {
        Usuario criador = usuarioComDadosPessoais();
        Evento existente = new Evento();
        existente.setId(10L);
        existente.setTitulo("Feira de ciencia");
        existente.setCriadoPor(criador);

        given(repository.findById(10L)).willReturn(Optional.of(existente));
        given(repository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Evento corpo = new Evento();
        corpo.setTitulo("Feira de ciencia e tecnologia");
        corpo.setDataHorario(LocalDateTime.of(2026, 9, 23, 19, 0));
        corpo.setConcluido(true);

        Evento salvo = service.atualizar(10L, corpo);

        assertThat(salvo.getCriadoPor()).isSameAs(criador);
    }

    @Test
    @DisplayName("criar um evento novo continua funcionando sem criador")
    void criacaoSemCriador() {
        given(repository.save(any())).willAnswer(chamada -> chamada.getArgument(0));

        Evento evento = new Evento();
        evento.setTitulo("Reuniao de pais");
        evento.setDataHorario(LocalDateTime.of(2026, 9, 25, 18, 0));

        Evento salvo = service.salvar(evento);

        assertThat(salvo.getTitulo()).isEqualTo("Reuniao de pais");
        assertThat(salvo.getConcluido()).isFalse();
        assertThat(salvo.getCriadoPor()).isNull();
    }

    private static Usuario usuarioComDadosPessoais() {
        Endereco endereco = new Endereco();
        endereco.setLogradouro("Rua das Flores");
        endereco.setNumero("100");

        Pessoa pessoa = new Pessoa();
        pessoa.setId(900L);
        pessoa.setNome("Administrador da Escola");
        pessoa.setCpf("123.456.789-00");
        pessoa.setEmail("admin@escola.test");
        pessoa.setEndereco(endereco);

        Escola escola = new Escola();
        escola.setId(1L);
        escola.setNome("Escola de Teste");
        escola.setStatus(StatusAtivoInativo.ATIVO);

        Usuario usuario = new Usuario();
        usuario.setId(500L);
        usuario.setPessoa(pessoa);
        usuario.setEscola(escola);
        usuario.setUsername("admin");
        usuario.setTipoUsuario(TipoUsuario.ADMINISTRADOR);
        usuario.setStatus(StatusAtivoInativo.ATIVO);
        return usuario;
    }
}
