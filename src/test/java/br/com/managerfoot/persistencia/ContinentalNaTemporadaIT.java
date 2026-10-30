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
import br.com.managerfoot.temporada.Continental;
import br.com.managerfoot.temporada.GrupoContinental;

/** Grupos da continental sorteados e gravados no início da temporada (SPEC §8.3.6, V34). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "managerfoot.continental.ativa=true")
class ContinentalNaTemporadaIT {

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
	ContinentalRepositorio continentais;

	@Autowired
	JdbcClient jdbc;

	long jogo;
	long temporada;

	@BeforeEach
	void criarJogoNoBrasil() {
		bancoPadrao.carregar(new GeradorBancoPadrao(ParametrosBancoPadrao.padrao(), ParametrosMotor.padrao()).gerarBanco());
		CampeonatoDisponivel brasil = moldes.campeonatosDisponiveis(bancoPadrao.moldeId().orElseThrow()).stream()
			.filter(c -> c.codigoPais().equals("BRA")).findFirst().orElseThrow();
		jogo = jogos.criarJogoComClubeSorteado(brasil.paisId(), LigaDeTeste.participante(contas).id(), 5);
		LigaDeTeste.escalarTodosPelaMaquina(jdbc, jogo);
		temporadas.iniciarPrimeiraTemporada(jogo, 2026);
		temporada = jdbc.sql("SELECT id FROM jogo_temporada WHERE jogo_id = :j").param("j", jogo).query(Long.class).single();
	}

	Map<Long, String> paises() {
		return jdbc.sql("SELECT id, coalesce(pais_codigo, 'BRA') AS pais FROM jogo_clube WHERE jogo_id = :jogo")
			.param("jogo", jogo)
			.query((rs, n) -> Map.entry(rs.getLong("id"), rs.getString("pais")))
			.list()
			.stream()
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Test
	void primeiraTemporadaTemOitoGruposComAsVagasDeCadaPaisSemRepetirPais() {
		Continental continental = continentais.carregar(jogo, temporada).orElseThrow();
		Map<Long, String> paises = paises();

		assertThat(continental.grupos()).hasSize(8);
		Map<String, Long> porPais = continental.grupos().stream().flatMap(g -> g.clubes().stream())
			.collect(Collectors.groupingBy(c -> paises.get(c.id()), Collectors.counting()));
		assertThat(porPais).containsExactlyInAnyOrderEntriesOf(Map.of("BRA", 7L, "ARG", 6L, "CHI", 3L, "COL", 3L, "URU", 3L,
				"BOL", 2L, "ECU", 2L, "PAR", 2L, "PER", 2L, "VEN", 2L));
		for (GrupoContinental grupo : continental.grupos()) {
			assertThat(grupo.clubes()).extracting(c -> paises.get(c.id())).doesNotHaveDuplicates();
			assertThat(grupo.partidas()).hasSize(12).allMatch(p -> p.placar().isEmpty());
		}
		// os brasileiros vêm da 1ª divisão
		List<Long> primeira = jogos.carregarLiga(jogo).divisoes().getFirst().clubes().stream().map(Clube::id).toList();
		assertThat(continental.grupos().stream().flatMap(g -> g.clubes().stream())
			.filter(c -> paises.get(c.id()).equals("BRA"))).allMatch(c -> primeira.contains(c.id()));
		assertThat(continental.mataMata()).isEmpty();
		assertThat(continentais.quadro(temporada)).hasSize(13).first()
			.matches(e -> e.depoisDaRodada() == 2 && e.jogoDoGrupo() == 1);
		assertThat(jdbc.sql("SELECT DISTINCT rodada FROM jogo_partida WHERE temporada_id = :t AND grupo IS NOT NULL ORDER BY rodada")
			.param("t", temporada).query(Integer.class).list()).containsExactly(2, 4, 7, 9, 14, 16);
	}

	@Test
	void ligaECopaNaoVeemAsPartidasDosGrupos() {
		assertThat(rodadas.totalDeRodadas(jogo)).isEqualTo(38);
		RodadasDeTeste.jogarRodada(rodadas, jogo, 1);
		RodadasDeTeste.jogarRodada(rodadas, jogo, 2);

		assertThat(rodadas.proximo(jogo)).isEqualTo(new RodadaRepositorio.Proximo.Liga(3));
		assertThat(jdbc.sql("SELECT count(*) FROM jogo_partida WHERE temporada_id = :t AND grupo IS NOT NULL AND gols_mandante IS NOT NULL")
			.param("t", temporada).query(Long.class).single()).isZero();
		assertThat(rodadas.classificacao(jogo).get(1).linhas()).hasSize(20).allMatch(l -> l.jogos() == 2);
		assertThat(rodadas.classificacao(jogo).get(2).linhas()).hasSize(20).allMatch(l -> l.jogos() == 2);
	}
}
