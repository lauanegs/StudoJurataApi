package studojurata_api.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Correção 2.8 da Segunda Análise Crítica: a tela Home (todos os perfis) e a
 * tela "Eventos" do Documento de Interfaces (CRUD completo: título, data,
 * horário, descrição, concluído) não tinham nenhuma entidade correspondente
 * — "tela fantasma" sem persistência. Apenas o Administrador cria/edita/
 * exclui eventos (ver SecurityConfig e Documento de Interfaces).
 */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class Evento extends BaseEntity {

    @Column(nullable = false)
    private String titulo;

    @Column(length = 2000)
    private String descricao;

    @Column(nullable = false)
    private LocalDateTime dataHorario;

    @Column(nullable = false)
    private Boolean concluido = false;

    /** Usuario (Administrador) que criou o evento. */
    @ManyToOne
    private Usuario criadoPor;
}
