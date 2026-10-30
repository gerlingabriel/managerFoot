package br.com.managerfoot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import br.com.managerfoot.bancopadrao.ContinentalPadrao;
import br.com.managerfoot.bancopadrao.GeradorBancoPadrao;
import br.com.managerfoot.bancopadrao.GeradorBancoPadrao.BancoGerado;
import br.com.managerfoot.bancopadrao.ParametrosBancoPadrao;
import br.com.managerfoot.contas.Usuario;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.persistencia.BancoPadraoRepositorio;
import br.com.managerfoot.persistencia.ContasRepositorio;
import br.com.managerfoot.persistencia.JogoRepositorio;
import br.com.managerfoot.persistencia.MoldeRepositorio;
import br.com.managerfoot.persistencia.MoldeRepositorio.CampeonatoDisponivel;

/** {@link ScriptBancoPadrao} e {@link BancoPadraoRepositorio} contra PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ScriptBancoPadraoIT {

	@Autowired
	BancoPadraoRepositorio repositorio;

	@Autowired
	MoldeRepositorio moldes;

	@Autowired
	JogoRepositorio jogos;

	@Autowired
	ContasRepositorio contas;

	@Autowired
	JdbcClient jdbc;

	@Test
	void carregaOsSeisCampeonatosUmaVezSoEDaParaCriarJogoComEles() {
		ScriptBancoPadrao script = new ScriptBancoPadrao(repositorio);
		script.run(new DefaultApplicationArguments("--carregar-banco-padrao"));
		long molde = repositorio.moldeId().orElseThrow();
		long jogadores = contarJogadores(molde);

		script.run(new DefaultApplicationArguments("--carregar-banco-padrao"));

		assertThat(jdbc.sql("SELECT count(*) FROM molde WHERE nome = :nome")
			.param("nome", BancoPadraoRepositorio.NOME_MOLDE)
			.query(Long.class)
			.single()).isEqualTo(1);
		assertThat(contarJogadores(molde)).isEqualTo(jogadores).isEqualTo((44 * 4 + 43 + 39 + 72 + 44) * 20L);

		List<CampeonatoDisponivel> campeonatos = moldes.campeonatosDisponiveis(molde);
		assertThat(campeonatos).extracting(CampeonatoDisponivel::codigoPais)
			.containsExactlyInAnyOrder("BRA", "ARG", "ENG", "ESP", "ITA", "POR");

		assertThat(moldes.continentais(molde)).extracting(MoldeRepositorio.ContinentalMolde::regiao)
			.containsExactly("AMERICA_DO_SUL", "EUROPA");
		assertThat(moldes.continentais(molde).getFirst().vagas()).containsExactlyEntriesOf(
				ContinentalPadrao.AMERICA_DO_SUL.vagas());
		assertThat(clubesSemLiga(molde, "VEN")).isEqualTo(4);
		assertThat(clubesSemLiga(molde, "GER")).isEqualTo(8);

		String sufixo = System.nanoTime() + "@exemplo.com";
		Usuario admin = contas.criarAdministrador("admin-banco-" + sufixo, "Admin", "senha-de-teste");
		Usuario participante = contas.cadastrar(contas.gerarLinkCadastro(admin.id()), "participante-banco-" + sufixo,
				"Participante", "senha-de-teste");
		for (CampeonatoDisponivel campeonato : campeonatos) {
			long jogo = jogos.criarJogoComClubeSorteado(campeonato.paisId(), participante.id(), 11);
			assertThat(jogos.clubeDoTreinador(jogo, participante.id())).as(campeonato.nomePais()).isPresent();
		}
	}

	@Test
	void semAOpcaoNaoCarregaNada() {
		BancoPadraoRepositorio semUso = new BancoPadraoRepositorio(jdbc, moldes) {
			@Override
			public Carga carregar(BancoGerado banco) {
				throw new AssertionError("não deveria carregar sem a opção");
			}
		};
		new ScriptBancoPadrao(semUso).run(new DefaultApplicationArguments());
	}

	@Test
	void moldeDeVersaoAntigaEhGravadoDeNovoSemMexerNosJogosJaCriados() {
		ScriptBancoPadrao script = new ScriptBancoPadrao(repositorio);
		script.run(new DefaultApplicationArguments("--carregar-banco-padrao"));
		long antigo = repositorio.moldeId().orElseThrow();
		String sufixo = System.nanoTime() + "@exemplo.com";
		Usuario admin = contas.criarAdministrador("admin-versao-" + sufixo, "Admin", "senha-de-teste");
		Usuario participante = contas.cadastrar(contas.gerarLinkCadastro(admin.id()), "participante-versao-" + sufixo,
				"Participante", "senha-de-teste");
		long jogo = jogos.criarJogoComClubeSorteado(moldes.campeonatosDisponiveis(antigo).getFirst().paisId(),
				participante.id(), 11);
		jdbc.sql("UPDATE molde SET versao_banco_padrao = 1 WHERE id = :molde").param("molde", antigo).update();

		BancoPadraoRepositorio.Carga carga = repositorio.carregar(
				new GeradorBancoPadrao(ParametrosBancoPadrao.padrao(), ParametrosMotor.padrao()).gerarBanco());

		assertThat(carga.criado()).isTrue();
		assertThat(carga.substituido()).isTrue();
		assertThat(carga.moldeId()).isNotEqualTo(antigo).isEqualTo(repositorio.moldeId().orElseThrow());
		assertThat(jdbc.sql("SELECT count(*) FROM molde WHERE id = :molde").param("molde", antigo).query(Long.class).single())
			.isZero();
		assertThat(jdbc.sql("SELECT versao_banco_padrao FROM molde WHERE id = :molde")
			.param("molde", carga.moldeId())
			.query(Integer.class)
			.single()).isEqualTo(BancoPadraoRepositorio.VERSAO);
		assertThat(jogos.clubeDoTreinador(jogo, participante.id())).isPresent();
	}

	/** Clubes de um país sem liga no molde; -1 se o país tem liga. */
	long clubesSemLiga(long molde, String codigo) {
		return jdbc.sql("""
				SELECT CASE WHEN p.nome_liga IS NULL THEN count(c.id) ELSE -1 END FROM molde_pais p
				LEFT JOIN molde_clube c ON c.pais_id = p.id
				WHERE p.molde_id = :molde AND p.codigo = :codigo GROUP BY p.nome_liga""")
			.param("molde", molde)
			.param("codigo", codigo)
			.query(Long.class)
			.single();
	}

	long contarJogadores(long molde) {
		return jdbc.sql("""
				SELECT count(*) FROM molde_jogador j
				JOIN molde_clube c ON c.id = j.clube_id
				JOIN molde_pais p ON p.id = c.pais_id
				WHERE p.molde_id = :molde""")
			.param("molde", molde)
			.query(Long.class)
			.single();
	}
}
