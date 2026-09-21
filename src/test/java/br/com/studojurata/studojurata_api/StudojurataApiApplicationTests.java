package br.com.studojurata.studojurata_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import studojurata_api.StudojurataApiApplication;
import studojurata_api.ia.job.GeracaoAutomaticaBaixoAproveitamentoJob;
import studojurata_api.ia.job.GeracaoAutomaticaSimuladoJob;
import studojurata_api.repository.gamificacao.SkinRepository;

/**
 * Sobe o contexto completo da aplicação — a rede de proteção mais básica do
 * backend, que até então falhava (o pacote deste teste não continha a classe
 * de configuração e o Spring não a encontrava).
 *
 * <p>O pacote continua o mesmo de antes por opção consciente: a classe é
 * resolvida explicitamente por {@code classes}, sem depender de varredura de
 * pacote.
 *
 * <p>Três beans com efeito colateral no boot são substituídos por dublês para
 * que o teste rode <b>sem banco de dados</b>:
 * <ul>
 *   <li>{@link SkinRepository} — {@code GamificacaoSeeder} é um
 *       {@code CommandLineRunner} sem {@code @Profile}, então grava o catálogo
 *       de skins em toda subida; sem o dublê, o boot exigiria PostgreSQL;</li>
 *   <li>{@code GeracaoAutomaticaBaixoAproveitamentoJob} — o
 *       {@code @Scheduled(fixedDelay)} dele dispara logo após o startup e
 *       consulta {@code alunoRepository.findAll()} fora de qualquer
 *       try/catch, o que geraria erro de banco em thread de background;</li>
 *   <li>{@code GeracaoAutomaticaSimuladoJob} — cron diário; dublado para o
 *       resultado não depender do horário em que a suíte roda.</li>
 * </ul>
 *
 * <p>{@code spring.jpa.hibernate.ddl-auto=none} desliga o DDL automático:
 * como {@code application.properties} já define o dialeto explicitamente, o
 * Hibernate não precisa abrir conexão para montar o {@code EntityManagerFactory}.
 */
@SpringBootTest(
		classes = StudojurataApiApplication.class,
		properties = "spring.jpa.hibernate.ddl-auto=none")
class StudojurataApiApplicationTests {

	@MockBean
	SkinRepository skinRepository;

	@MockBean
	GeracaoAutomaticaBaixoAproveitamentoJob geracaoAutomaticaBaixoAproveitamentoJob;

	@MockBean
	GeracaoAutomaticaSimuladoJob geracaoAutomaticaSimuladoJob;

	@Test
	void contextLoads() {
	}

}
