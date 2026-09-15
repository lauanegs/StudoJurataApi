package studojurata_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Correção 2.9 da Segunda Análise Crítica: habilita o JPA Auditing
 * (createdAt/updatedAt automáticos em BaseEntity, ver model/BaseEntity.java)
 * para toda a aplicação.
 *
 * @EnableScheduling habilita o @Scheduled de GeracaoAutomaticaSimuladoJob
 * (ia/job) — geração de simulado de reforço por repetição espaçada, sem
 * intervenção do professor (item 1.4 da Análise Crítica).
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class StudojurataApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(StudojurataApiApplication.class, args);
	}

}
