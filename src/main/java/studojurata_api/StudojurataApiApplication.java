package studojurata_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Correção 2.9 da Segunda Análise Crítica: habilita o JPA Auditing
 * (createdAt/updatedAt automáticos em BaseEntity, ver model/BaseEntity.java)
 * para toda a aplicação.
 */
@SpringBootApplication
@EnableJpaAuditing
public class StudojurataApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(StudojurataApiApplication.class, args);
	}

}
