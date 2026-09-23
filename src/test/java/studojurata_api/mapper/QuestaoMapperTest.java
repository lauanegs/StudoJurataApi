package studojurata_api.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import studojurata_api.dto.QuestaoRequestDTO;
import studojurata_api.dto.QuestaoResponseDTO;
import studojurata_api.exception.RecursoNaoEncontradoException;
import studojurata_api.model.Disciplina;
import studojurata_api.model.Questao;
import studojurata_api.model.enums.NivelDificuldade;
import studojurata_api.model.enums.OrigemQuestao;
import studojurata_api.model.enums.StatusQuestao;
import studojurata_api.model.enums.TipoQuestao;
import studojurata_api.repository.DisciplinaRepository;

/**
 * C2.7d — o mapper não é mais um caminho para o cliente definir a moderação:
 * {@code origem} vem do corpo, mas não é copiada para a entidade (quem decide é
 * o {@code QuestaoService}). O status nunca teve campo no corpo.
 */
@ExtendWith(MockitoExtension.class)
class QuestaoMapperTest {

    private static final long DISC_A = 10L;

    @Mock private DisciplinaRepository disciplinaRepository;

    private QuestaoMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new QuestaoMapper(disciplinaRepository);
    }

    @Test
    @DisplayName("origem enviada pelo cliente não chega na entidade")
    void origemDoClienteEhIgnorada() {
        QuestaoRequestDTO dto = new QuestaoRequestDTO();
        dto.setEnunciado("Quanto é 2 + 2?");
        dto.setTipo(TipoQuestao.ALTERNATIVAS);
        dto.setNivelDificuldade(NivelDificuldade.FACIL);
        dto.setOrigem(OrigemQuestao.IA);

        Questao questao = mapper.toEntity(dto);

        assertThat(questao.getOrigem()).isNull();
        assertThat(questao.getStatus()).isNull();
        assertThat(questao.getEnunciado()).isEqualTo("Quanto é 2 + 2?");
        assertThat(questao.getNivelDificuldade()).isEqualTo(NivelDificuldade.FACIL);
        verifyNoInteractions(disciplinaRepository);
    }

    @Test
    @DisplayName("disciplina informada é resolvida para a entidade")
    void disciplinaEhResolvida() {
        given(disciplinaRepository.findById(DISC_A)).willReturn(Optional.of(disciplina()));

        assertThat(mapper.toEntity(dtoComDisciplina()).getDisciplina().getId()).isEqualTo(DISC_A);
    }

    @Test
    @DisplayName("disciplina inexistente continua 404")
    void disciplinaInexistenteResponde404() {
        given(disciplinaRepository.findById(DISC_A)).willReturn(Optional.empty());

        try {
            mapper.toEntity(dtoComDisciplina());
            fail("esperava RecursoNaoEncontradoException");
        } catch (RecursoNaoEncontradoException esperado) {
            assertThat(esperado.getMessage()).contains(String.valueOf(DISC_A));
        }
    }

    @Test
    @DisplayName("sem disciplina a questão fica sem vínculo (exclusiva do administrador)")
    void semDisciplinaFicaSemVinculo() {
        QuestaoRequestDTO dto = new QuestaoRequestDTO();
        dto.setEnunciado("Enunciado");
        dto.setTipo(TipoQuestao.VERDADEIRO_FALSO);

        assertThat(mapper.toEntity(dto).getDisciplina()).isNull();

        verifyNoInteractions(disciplinaRepository);
    }

    @Test
    @DisplayName("resposta expõe origem e status do registro, não do cliente")
    void respostaExpoeOrigemEStatus() {
        Questao questao = new Questao();
        questao.setId(1L);
        questao.setOrigem(OrigemQuestao.IA);
        questao.setStatus(StatusQuestao.PENDENTE);

        QuestaoResponseDTO dto = mapper.toResponseDTO(questao);

        assertThat(dto.getOrigem()).isEqualTo(OrigemQuestao.IA);
        assertThat(dto.getStatus()).isEqualTo(StatusQuestao.PENDENTE);
        assertThat(dto.getDisciplinaId()).isNull();
    }

    private static QuestaoRequestDTO dtoComDisciplina() {
        QuestaoRequestDTO dto = new QuestaoRequestDTO();
        dto.setEnunciado("Enunciado");
        dto.setTipo(TipoQuestao.ALTERNATIVAS);
        dto.setDisciplinaId(DISC_A);
        return dto;
    }

    private static Disciplina disciplina() {
        Disciplina disciplina = new Disciplina();
        disciplina.setId(DISC_A);
        disciplina.setTitulo("Matemática");
        return disciplina;
    }
}
