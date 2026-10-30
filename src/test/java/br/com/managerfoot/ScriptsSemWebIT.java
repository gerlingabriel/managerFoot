package br.com.managerfoot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import br.com.managerfoot.persistencia.BancoPadraoRepositorio;

/** Os comandos do README rodam com {@code web-application-type=none}: a aplicação sobe sem o ambiente web. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = "spring.main.web-application-type=none")
class ScriptsSemWebIT {

	@Autowired
	ApplicationContext contexto;

	@Test
	void sobeSemOAmbienteWeb() {
		assertThat(contexto.getBean(BancoPadraoRepositorio.class)).isNotNull();
	}
}
