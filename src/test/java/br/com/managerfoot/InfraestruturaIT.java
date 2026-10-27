package br.com.managerfoot;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/**
 * Verifica a base técnica (Etapa 0): aplicação sobe com PostgreSQL real,
 * Flyway aplica as migrações e o health check responde.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InfraestruturaIT {

	@LocalServerPort
	int porta;

	@Autowired
	Flyway flyway;

	@Test
	void flywayAplicaMigracaoInicial() {
		assertThat(flyway.info().applied())
			.extracting(m -> m.getVersion().getVersion())
			.contains("1");
		assertThat(flyway.info().pending()).isEmpty();
	}

	@Test
	void healthCheckRespondeUp() throws Exception {
		HttpResponse<String> resposta = HttpClient.newHttpClient().send(
				HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/actuator/health")).build(),
				HttpResponse.BodyHandlers.ofString());

		assertThat(resposta.statusCode()).isEqualTo(200);
		assertThat(resposta.body()).contains("\"status\":\"UP\"");
	}

}
