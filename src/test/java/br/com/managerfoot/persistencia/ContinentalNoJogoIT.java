package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.bancopadrao.GeradorBancoPadrao;
import br.com.managerfoot.bancopadrao.ParametrosBancoPadrao;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.motor.ParametrosMotor;
import br.com.managerfoot.persistencia.MoldeRepositorio.CampeonatoDisponivel;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Divisao;

/** Clubes estrangeiros e vagas copiados para o jogo (SPEC §8.3.6, V33) contra PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ContinentalNoJogoIT {

	@Autowired
	BancoPadraoRepositorio bancoPadrao;

	@Autowired
	MoldeRepositorio moldes;

	@Autowired
	JogoRepositorio jogos;

	@Autowired
	ContasRepositorio contas;

	@Autowired
	TemporadaRepositorio temporadas;

	@Autowired
	RodadaRepositorio rodadas;

	@Autowired
	JdbcClient jdbc;

	long molde;

	@BeforeEach
	void carregarBancoPadrao() {
		bancoPadrao.carregar(new GeradorBancoPadrao(ParametrosBancoPadrao.padrao(), ParametrosMotor.padrao()).gerarBanco());
		molde = bancoPadrao.moldeId().orElseThrow();
	}

	long criarJogo(String codigo) {
		CampeonatoDisponivel campeonato = moldes.campeonatosDisponiveis(molde).stream()
			.filter(c -> c.codigoPais().equals(codigo)).findFirst().orElseThrow();
		return jogos.criarJogoComClubeSorteado(campeonato.paisId(), LigaDeTeste.participante(contas).id(), 5);
	}

	Map<String, Long> estrangeirosPorPais(long jogo) {
		return jdbc.sql("SELECT pais_codigo FROM jogo_clube WHERE jogo_id = :jogo AND pais_codigo IS NOT NULL")
			.param("jogo", jogo)
			.query(String.class)
			.list()
			.stream()
			.collect(Collectors.groupingBy(c -> c, Collectors.counting()));
	}

	@Test
	void jogoNoBrasilCopiaAsVagasDaAmericaDoSulEOsClubesQuePodemOcupalas() {
		long jogo = criarJogo("BRA");

		assertThat(jdbc.sql("SELECT regiao FROM jogo_continental WHERE jogo_id = :jogo").param("jogo", jogo)
			.query(String.class).single()).isEqualTo("AMERICA_DO_SUL");
		assertThat(jdbc.sql("SELECT pais_codigo FROM jogo_continental_vaga WHERE jogo_id = :jogo ORDER BY ordem")
			.param("jogo", jogo).query(String.class).list())
			.containsExactly("BRA", "ARG", "CHI", "COL", "URU", "BOL", "ECU", "PAR", "PER", "VEN");
		// Argentina: os 20 mais fortes, o tamanho da 1ª divisão dela; gerados: o dobro das vagas
		assertThat(estrangeirosPorPais(jogo)).containsExactlyInAnyOrderEntriesOf(Map.of("ARG", 20L, "CHI", 6L, "COL", 6L,
				"URU", 6L, "BOL", 4L, "ECU", 4L, "PAR", 4L, "PER", 4L, "VEN", 4L));
		assertThat(jdbc.sql("""
				SELECT count(*) FROM jogo_jogador j JOIN jogo_clube c ON c.id = j.clube_id
				WHERE c.jogo_id = :jogo AND c.pais_codigo = 'ARG' AND j.nacionalidade = 'ARG'""")
			.param("jogo", jogo).query(Long.class).single()).isEqualTo(20 * 20L);
		int maisFracoCopiado = jdbc.sql("SELECT min(forca) FROM jogo_clube WHERE jogo_id = :jogo AND pais_codigo = 'ARG'")
			.param("jogo", jogo).query(Integer.class).single();
		long argentinosNoMoldeMaisFortes = jdbc.sql("""
				SELECT count(*) FROM molde_clube c JOIN molde_pais p ON p.id = c.pais_id
				WHERE p.molde_id = :molde AND p.codigo = 'ARG' AND c.forca > :forca""")
			.param("molde", molde).param("forca", maisFracoCopiado).query(Long.class).single();
		assertThat(argentinosNoMoldeMaisFortes).isLessThan(20);
	}

	@Test
	void jogoNaInglaterraCopiaAEuropaComPortugalEspanhaEItaliaPelaPrimeiraDivisao() {
		long jogo = criarJogo("ENG");
		assertThat(estrangeirosPorPais(jogo)).containsEntry("POR", 18L).containsEntry("ESP", 20L).containsEntry("ITA", 20L)
			.containsEntry("GER", 8L).containsEntry("NOR", 2L).doesNotContainKey("ENG").doesNotContainKey("BRA");
	}

	@Test
	void estrangeirosFicamForaDaLigaDoCaixaEDaEvolucao() {
		long jogo = criarJogo("BRA");
		ConfiguracaoLiga liga = jogos.carregarLiga(jogo);
		List<Long> daLiga = liga.divisoes().stream().flatMap(d -> d.clubes().stream()).map(Clube::id).toList();
		assertThat(liga.divisoes().stream().mapToInt(d -> d.clubes().size()).sum() + liga.filaDeEspera().size())
			.isEqualTo(44);
		assertThat(liga.filaDeEspera()).hasSize(4);
		assertThat(jogos.carregarPlanteis(jogo)).hasSize(44);
		String forcasAntes = forcasDosEstrangeiros(jogo);

		LigaDeTeste.escalarTodosPelaMaquina(jdbc, jogo);
		temporadas.iniciarPrimeiraTemporada(jogo, 2026);
		RodadasDeTeste.jogarRodada(rodadas, jogo, 1);
		RodadasDeTeste.jogarRodada(rodadas, jogo, 2);

		assertThat(jdbc.sql("""
				SELECT count(*) FROM jogo_clube WHERE jogo_id = :jogo AND pais_codigo IS NOT NULL
				  AND (saldo IS NOT NULL OR moral IS NOT NULL OR estadio_capacidade IS NOT NULL OR divisao_id IS NOT NULL)""")
			.param("jogo", jogo).query(Long.class).single()).isZero();
		assertThat(forcasDosEstrangeiros(jogo)).isEqualTo(forcasAntes);
		assertThat(jdbc.sql("""
				SELECT count(*) FROM jogo_partida p JOIN jogo_temporada t ON t.id = p.temporada_id
				JOIN jogo_clube c ON c.id IN (p.mandante_id, p.visitante_id)
				WHERE t.jogo_id = :jogo AND c.pais_codigo IS NOT NULL""")
			.param("jogo", jogo).query(Long.class).single()).isZero();
		assertThat(daLiga).hasSize(40);
		assertThat(liga.divisoes()).extracting(Divisao::prioridade).containsExactly(1, 2);
	}

	String forcasDosEstrangeiros(long jogo) {
		return jdbc.sql("""
				SELECT string_agg(c.forca || ':' || j.forca || ':' || j.a_venda, ',' ORDER BY j.id)
				FROM jogo_jogador j JOIN jogo_clube c ON c.id = j.clube_id
				WHERE c.jogo_id = :jogo AND c.pais_codigo IS NOT NULL""")
			.param("jogo", jogo).query(String.class).single();
	}
}
