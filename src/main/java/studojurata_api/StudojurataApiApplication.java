package studojurata_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * JPA Auditing preenche createdAt/updatedAt de BaseEntity; o scheduling
 * executa os jobs de geração automática de simulados em ia/job.
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class StudojurataApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(StudojurataApiApplication.class, args);
	}

}
