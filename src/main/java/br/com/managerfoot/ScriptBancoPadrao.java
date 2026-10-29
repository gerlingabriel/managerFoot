package br.com.managerfoot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import br.com.managerfoot.bancopadrao.GeradorBancoPadrao;
import br.com.managerfoot.bancopadrao.PaisPadrao;
import br.com.managerfoot.bancopadrao.ParametrosBancoPadrao;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.persistencia.BancoPadraoRepositorio;

/**
 * Com a opção {@code --carregar-banco-padrao}, gera e grava o banco padrão (SPEC §4.1;
 * docs/decisions/0008). Rodar de novo não duplica nada. Ver o README.
 */
@Component
class ScriptBancoPadrao implements ApplicationRunner {

	static final String OPCAO = "carregar-banco-padrao";
	private static final Logger LOG = LoggerFactory.getLogger(ScriptBancoPadrao.class);

	private final BancoPadraoRepositorio repositorio;

	ScriptBancoPadrao(BancoPadraoRepositorio repositorio) {
		this.repositorio = repositorio;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!args.containsOption(OPCAO)) {
			return;
		}
		GeradorBancoPadrao gerador = new GeradorBancoPadrao(ParametrosBancoPadrao.padrao(), ParametrosMotor.padrao());
		BancoPadraoRepositorio.Carga carga = repositorio.carregar(gerador.gerar(PaisPadrao.TODOS));
		if (carga.substituido()) {
			LOG.info("Banco padrão de versão antiga substituído pela versão {} no molde {}; jogos já criados não mudam.",
					BancoPadraoRepositorio.VERSAO, carga.moldeId());
		} else if (carga.criado()) {
			LOG.info("Banco padrão gravado no molde {} com {} países.", carga.moldeId(), PaisPadrao.TODOS.size());
		} else {
			LOG.info("Banco padrão já estava carregado no molde {}; nada foi gravado.", carga.moldeId());
		}
	}
}
