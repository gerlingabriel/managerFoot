package br.com.managerfoot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import br.com.managerfoot.persistencia.ContasRepositorio;

/** {@link ScriptLinkCadastro} com a saída capturada. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ScriptLinkCadastroIT {

	@Autowired
	ContasRepositorio contas;

	@Test
	void mostraUmLinkQueAbreOCadastro() {
		String email = "link" + System.nanoTime() + "@exemplo.com";
		contas.criarAdministrador(email, "Dono", "senha-do-admin");
		ByteArrayOutputStream saida = new ByteArrayOutputStream();

		new ScriptLinkCadastro(contas, "http://jogo.local/", new PrintStream(saida, true, StandardCharsets.UTF_8))
			.run(new DefaultApplicationArguments("--gerar-link-cadastro=" + email));

		String texto = saida.toString(StandardCharsets.UTF_8).strip();
		assertThat(texto).contains("http://jogo.local/cadastro?convite=");
		String token = texto.substring(texto.indexOf("convite=") + "convite=".length());
		assertThat(contas.validadeDoLink(token)).isPresent();
	}

	@Test
	void recusaEMailSemContaOuParticipante() {
		ByteArrayOutputStream saida = new ByteArrayOutputStream();
		ScriptLinkCadastro script = new ScriptLinkCadastro(contas, "http://jogo.local", new PrintStream(saida));

		assertThatIllegalStateException()
			.isThrownBy(() -> script.run(new DefaultApplicationArguments("--gerar-link-cadastro=ninguem@exemplo.com")))
			.withMessageContaining("ninguem@exemplo.com");
		assertThatIllegalStateException()
			.isThrownBy(() -> script.run(new DefaultApplicationArguments("--gerar-link-cadastro")));
		new ScriptLinkCadastro(contas, "http://jogo.local", new PrintStream(saida)).run(new DefaultApplicationArguments());
		assertThat(saida.size()).isZero();
	}
}
