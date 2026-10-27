package br.com.managerfoot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import br.com.managerfoot.contas.Papel;
import br.com.managerfoot.persistencia.ContasRepositorio;

/** {@link ScriptAdministrador} com variáveis de ambiente simuladas. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ScriptAdministradorIT {

	@Autowired
	ContasRepositorio contas;

	@Test
	void criaAdministradorComAsVariaveisDeAmbiente() {
		String email = "script" + System.nanoTime() + "@exemplo.com";
		Map<String, String> ambiente = Map.of("ADMIN_EMAIL", email, "ADMIN_NOME", "Dono", "ADMIN_SENHA", "senha-do-script");

		new ScriptAdministrador(contas, ambiente::get).run(new DefaultApplicationArguments("--criar-admin"));

		assertThat(contas.autenticar(email, "senha-do-script")).hasValueSatisfying(
				u -> assertThat(u.papel()).isEqualTo(Papel.ADMINISTRADOR));
	}

	@Test
	void semAOpcaoNaoFazNadaESemVariaveisDizQuaisFaltam() {
		new ScriptAdministrador(contas, v -> null).run(new DefaultApplicationArguments());

		assertThatIllegalStateException()
			.isThrownBy(() -> new ScriptAdministrador(contas, Map.of("ADMIN_NOME", "Dono")::get)
				.run(new DefaultApplicationArguments("--criar-admin")))
			.withMessageContaining("ADMIN_EMAIL")
			.withMessageContaining("ADMIN_SENHA")
			.withMessageNotContaining("ADMIN_NOME");
	}
}
