package br.com.managerfoot;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.persistencia.ContasRepositorio;

/**
 * Script de administração (SPEC §3.4; docs/decisions/0007): com a opção {@code --criar-admin}, cria
 * um administrador a partir das variáveis de ambiente {@code ADMIN_EMAIL}, {@code ADMIN_NOME} e
 * {@code ADMIN_SENHA}. A senha não vai na linha de comando nem no log. Ver o README.
 */
@Component
class ScriptAdministrador implements ApplicationRunner {

	static final String OPCAO = "criar-admin";
	private static final Logger LOG = LoggerFactory.getLogger(ScriptAdministrador.class);

	private final ContasRepositorio contas;
	private final UnaryOperator<String> ambiente;

	@Autowired
	ScriptAdministrador(ContasRepositorio contas) {
		this(contas, System::getenv);
	}

	ScriptAdministrador(ContasRepositorio contas, UnaryOperator<String> ambiente) {
		this.contas = contas;
		this.ambiente = ambiente;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!args.containsOption(OPCAO)) {
			return;
		}
		List<String> faltam = new ArrayList<>();
		for (String variavel : List.of("ADMIN_EMAIL", "ADMIN_NOME", "ADMIN_SENHA")) {
			String valor = ambiente.apply(variavel);
			if (valor == null || valor.isBlank()) {
				faltam.add(variavel);
			}
		}
		if (!faltam.isEmpty()) {
			throw new IllegalStateException("--" + OPCAO + " precisa das variáveis de ambiente: " + String.join(", ", faltam));
		}
		Usuario admin = contas.criarAdministrador(ambiente.apply("ADMIN_EMAIL"), ambiente.apply("ADMIN_NOME"),
				ambiente.apply("ADMIN_SENHA"));
		LOG.info("Administrador criado com id {}.", admin.id());
	}
}
