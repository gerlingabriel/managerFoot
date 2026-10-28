package br.com.managerfoot;

import java.io.PrintStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.persistencia.ContasRepositorio;

/**
 * Com {@code --gerar-link-cadastro=<e-mail do administrador>}, gera um link de cadastro e o mostra
 * no terminal (SPEC §3.4; docs/decisions/0009). O link não vai para o log: quem o tiver cria uma
 * conta. Fica até existir a tela do administrador. Ver o README.
 */
@Component
class ScriptLinkCadastro implements ApplicationRunner {

	static final String OPCAO = "gerar-link-cadastro";

	private final ContasRepositorio contas;
	private final String urlBase;
	private final PrintStream saida;

	@Autowired
	ScriptLinkCadastro(ContasRepositorio contas, @Value("${managerfoot.url-base}") String urlBase) {
		this(contas, urlBase, System.out);
	}

	ScriptLinkCadastro(ContasRepositorio contas, String urlBase, PrintStream saida) {
		this.contas = contas;
		this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
		this.saida = saida;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!args.containsOption(OPCAO)) {
			return;
		}
		List<String> valores = args.getOptionValues(OPCAO);
		if (valores.size() != 1 || valores.getFirst().isBlank()) {
			throw new IllegalStateException("use --" + OPCAO + "=<e-mail do administrador>");
		}
		Usuario admin = contas.buscarPorEmail(valores.getFirst())
			.orElseThrow(() -> new IllegalStateException("não há conta com o e-mail " + valores.getFirst()));
		String token = contas.gerarLinkCadastro(admin.id());
		saida.println("Link de cadastro (uso único): " + urlBase + "/cadastro?convite="
				+ URLEncoder.encode(token, StandardCharsets.UTF_8));
	}
}
