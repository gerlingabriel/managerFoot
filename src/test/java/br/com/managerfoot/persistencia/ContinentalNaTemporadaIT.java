package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import br.com.managerfoot.temporada.Classificacao;
import br.com.managerfoot.temporada.Continental;
import br.com.managerfoot.temporada.FaseCopa;
import br.com.managerfoot.temporada.LinhaClassificacao;
import br.com.managerfoot.temporada.VagasContinental;
import br.com.managerfoot.temporada.GrupoContinental;

/** Grupos da continental sorteados e gravados no início da temporada (SPEC §8.3.6, V34). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
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
	FimDeTemporadaRepositorio fins;

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

		// depois da rodada 2 vem o 1º jogo dos grupos, e a rodada 3 espera
		assertThat(rodadas.proximo(jogo)).isInstanceOfSatisfying(RodadaRepositorio.Proximo.EtapaContinental.class,
				e -> assertThat(e.indice()).isZero());
		assertThatIllegalStateException().isThrownBy(() -> rodadas.avancar(jogo, 3)).withMessageContaining("continental");
		assertThat(jdbc.sql("SELECT count(*) FROM jogo_partida WHERE temporada_id = :t AND grupo IS NOT NULL AND gols_mandante IS NOT NULL")
			.param("t", temporada).query(Long.class).single()).isZero();
		RodadaRepositorio.EtapaContinentalJogada jogada = rodadas.avancarContinental(jogo, 0, List.of());
		assertThat(jogada.resultado().partidas()).hasSize(16);
		assertThat(rodadas.proximo(jogo)).isEqualTo(new RodadaRepositorio.Proximo.Liga(3));
		assertThatIllegalStateException().isThrownBy(() -> rodadas.avancarContinental(jogo, 0, List.of()));
		assertThat(jdbc.sql("SELECT count(*) FROM jogo_partida WHERE temporada_id = :t AND grupo IS NOT NULL AND gols_mandante IS NOT NULL")
			.param("t", temporada).query(Long.class).single()).isEqualTo(16);
		assertThat(rodadas.classificacao(jogo).get(1).linhas()).hasSize(20).allMatch(l -> l.jogos() == 2);
		assertThat(rodadas.classificacao(jogo).get(2).linhas()).hasSize(20).allMatch(l -> l.jogos() == 2);
	}

	String forcasDosEstrangeiros() {
		return jdbc.sql("""
				SELECT string_agg(j.forca::text, ',' ORDER BY j.id) FROM jogo_jogador j JOIN jogo_clube c ON c.id = j.clube_id
				WHERE c.jogo_id = :jogo AND c.pais_codigo IS NOT NULL""")
			.param("jogo", jogo).query(String.class).single();
	}

	@Test
	void temporadaInteiraComGruposMataMataFinalDepoisDaDaCopaCampeaoNoHistoricoENovaEdicaoPelaTabela() {
		String forcasAntes = forcasDosEstrangeiros();
		int total = rodadas.totalDeRodadas(jogo);
		for (int r = 1; r < total; r++) {
			RodadasDeTeste.semDecisoesDoTreinador(jdbc, jogo);
			RodadasDeTeste.jogarRodada(rodadas, jogo, r);
		}
		RodadasDeTeste.semDecisoesDoTreinador(jdbc, jogo);
		RodadasDeTeste.jogarCopaPendente(rodadas, jogo);
		assertThat(continentais.carregar(jogo, temporada).orElseThrow().mataMata().orElseThrow().faseAtual())
			.isEqualTo(FaseCopa.FINAL);
		rodadas.avancar(jogo, total);

		// rev. 77: última rodada da liga, depois a final da copa e por último a da continental
		assertThat(rodadas.proximo(jogo)).isInstanceOfSatisfying(RodadaRepositorio.Proximo.EtapaCopa.class,
				e -> assertThat(e.etapa().fase()).isEqualTo(FaseCopa.FINAL));
		assertThatIllegalStateException().isThrownBy(() -> fins.avancarTemporada(jogo, 1, 99));
		RodadaRepositorio.Proximo.EtapaCopa finalDaCopa = (RodadaRepositorio.Proximo.EtapaCopa) rodadas.proximo(jogo);
		rodadas.avancarCopa(jogo, finalDaCopa.indice(), List.of());
		RodadaRepositorio.Proximo depois = rodadas.proximo(jogo);
		assertThat(depois).isInstanceOfSatisfying(RodadaRepositorio.Proximo.EtapaContinental.class,
				e -> assertThat(e.etapa().nome()).isEqualTo("Final"));
		assertThatIllegalStateException().isThrownBy(() -> fins.avancarTemporada(jogo, 1, 99))
			.withMessageContaining("continental");
		RodadaRepositorio.EtapaContinentalJogada finalJogada = rodadas.avancarContinental(jogo,
				((RodadaRepositorio.Proximo.EtapaContinental) depois).indice(), List.of());
		assertThat(rodadas.proximo(jogo)).isEqualTo(new RodadaRepositorio.Proximo.EtapaMundial(0, FaseCopa.SEMIFINAL));

		Continental continental = continentais.carregar(jogo, temporada).orElseThrow();
		assertThat(continental).isEqualTo(finalJogada.continental());
		assertThat(continental.grupos()).allMatch(GrupoContinental::encerrado);
		assertThat(jdbc.sql("SELECT count(*) FROM jogo_copa_confronto WHERE temporada_id = :t AND competicao = 'CONTINENTAL'")
			.param("t", temporada).query(Long.class).single()).isEqualTo(15);
		Clube campeao = continental.campeao().orElseThrow();
		Map<String, Object> historico = jdbc.sql("SELECT clube_id, vice_id, clube_pais FROM jogo_campeao_continental WHERE jogo_id = :j")
			.param("j", jogo).query().singleRow();
		assertThat(historico.get("clube_id")).isEqualTo(campeao.id());
		assertThat(historico.get("vice_id")).isEqualTo(continental.vice().orElseThrow().id());
		assertThat(historico.get("clube_pais")).isEqualTo(paises().get(campeao.id()).equals("BRA") ? null : paises().get(campeao.id()));
		// os clubes do país jogado recebem bilheteria da continental; o estrangeiro não tem caixa nem evolui
		assertThat(jdbc.sql("""
				SELECT count(*) FROM jogo_lancamento WHERE temporada_id = :t AND competicao = 'CONTINENTAL' AND tipo = 'BILHETERIA'""")
			.param("t", temporada).query(Long.class).single()).isPositive();
		assertThat(jdbc.sql("""
				SELECT count(*) FROM jogo_lancamento l JOIN jogo_clube c ON c.id = l.clube_id
				WHERE l.temporada_id = :t AND c.pais_codigo IS NOT NULL""")
			.param("t", temporada).query(Long.class).single()).isZero();
		assertThat(jdbc.sql("""
				SELECT count(*) FROM jogo_clube WHERE jogo_id = :j AND pais_codigo IS NOT NULL
				  AND (saldo IS NOT NULL OR moral IS NOT NULL)""")
			.param("j", jogo).query(Long.class).single()).isZero();
		assertThat(forcasDosEstrangeiros()).isEqualTo(forcasAntes);

		// 2ª temporada: os 7 brasileiros saem da tabela da 1ª divisão com o campeão da copa
		Classificacao primeira = rodadas.classificacao(jogo).get(1);
		Optional<Clube> campeaoDaCopa = jdbc.sql("SELECT clube_id FROM jogo_campeao_copa WHERE jogo_id = :j")
			.param("j", jogo).query(Long.class).optional()
			.map(id -> jogos.carregarLiga(jogo).divisoes().stream().flatMap(d -> d.clubes().stream())
				.filter(c -> c.id() == id).findFirst()
				.orElseGet(() -> jogos.carregarLiga(jogo).filaDeEspera().stream().filter(c -> c.id() == id).findFirst().orElseThrow()));
		List<Long> esperados = VagasContinental.peloDesempenho(primeira.linhas().stream().map(LinhaClassificacao::clube).toList(),
				campeaoDaCopa, 7).stream().map(Clube::id).toList();
		RodadasDeTeste.semDecisoesDoTreinador(jdbc, jogo);
		RodadasDeTeste.jogarCopaPendente(rodadas, jogo);
		RodadasDeTeste.semDecisoesDoTreinador(jdbc, jogo);
		fins.avancarTemporada(jogo, 1, 99);
		Map<String, Object> liga = jdbc.sql("""
				SELECT vice_id, terceiro_id FROM jogo_campeao WHERE jogo_id = :j AND divisao_prioridade = 1""")
			.param("j", jogo).query().singleRow();
		assertThat(liga.get("vice_id")).isEqualTo(primeira.linhas().get(1).clube().id());
		assertThat(liga.get("terceiro_id")).isEqualTo(primeira.linhas().get(2).clube().id());
		long segunda = jdbc.sql("SELECT id FROM jogo_temporada WHERE jogo_id = :j AND numero = 2").param("j", jogo)
			.query(Long.class).single();
		Map<Long, String> paises = paises();
		List<Long> brasileiros = continentais.carregar(jogo, segunda).orElseThrow().grupos().stream()
			.flatMap(g -> g.clubes().stream()).filter(c -> paises.get(c.id()).equals("BRA")).map(Clube::id).toList();
		assertThat(brasileiros).containsExactlyInAnyOrderElementsOf(esperados);
	}
}
