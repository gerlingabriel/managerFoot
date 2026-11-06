package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.dominio.Temperamento;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Divisao;
import br.com.managerfoot.temporada.Temporada;

/** Migração V6 e {@link TemporadaRepositorio} contra PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TemporadaRepositorioIT {

	@Autowired
	TemporadaRepositorio temporadas;

	@Autowired
	JogoRepositorio jogos;

	@Autowired
	MoldeRepositorio moldes;

	@Autowired
	ContasRepositorio contas;

	@Autowired
	JdbcClient jdbc;

	long jogo;

	@BeforeEach
	void criaJogo() {
		long pais = moldes.criarPais(moldes.criarMolde("Padrão"), "BRA", "Brasil");
		// 2ª divisão com número ímpar de clubes: há folga em cada rodada
		ConfiguracaoLiga liga = LigaDeTeste.gravarNoMolde(moldes, pais, new ConfiguracaoLiga("Nacional",
				List.of(new Divisao("Série A", 1, 50, 0, 2, clubes("A", 16, 40)),
						new Divisao("Série B", 2, 30, 2, 2, clubes("B", 17, 25))),
				clubes("Fila", 2, 15)));
		for (Divisao divisao : liga.divisoes()) {
			for (Clube clube : divisao.clubes().subList(0, 2)) {
				moldes.adicionarJogadores(clube.id(), plantel());
			}
		}
		jogo = jogos.criarJogoComClubeSorteado(pais, LigaDeTeste.participante(contas).id(), 1);
	}

	@Test
	void temporadaGravadaVoltaIgualAosSorteiosDaSemente() {
		Temporada iniciada = temporadas.iniciarPrimeiraTemporada(jogo, 2026);

		// o bônus do treinador (rev. 121) é sorteado e gravado à parte
		assertThat(iniciada).isEqualTo(Temporada.iniciar(1, jogos.carregarLiga(jogo), jogos.carregarPlanteis(jogo), 2026,
				Set.of(clubeDoUsuario())).comBonusTreinador(iniciada.bonusTreinador()));
		assertThat(temporadas.carregarAtual(jogo)).isEqualTo(iniciada);
		assertThat(iniciada.divisoes().get(1).calendario().rodadas()).allMatch(r -> r.folga().isPresent());
		assertThat(iniciada.divisoes())
			.allMatch(d -> d.bonus().asteriscoTemporario().size() + d.bonus().bonusExtra().size() == 15);
		assertThat(contar("SELECT count(*) FROM jogo_partida p JOIN jogo_temporada t ON t.id = p.temporada_id "
				+ "WHERE t.jogo_id = ? AND p.confronto_id IS NULL")).isEqualTo(16 * 15 + 17 * 16);
	}

	@Test
	void segundaChamadaERecusadaSemDuplicarNada() {
		Temporada primeira = temporadas.iniciarPrimeiraTemporada(jogo, 1);

		assertThatThrownBy(() -> temporadas.iniciarPrimeiraTemporada(jogo, 2)).isInstanceOf(IllegalStateException.class);
		assertThat(temporadas.carregarAtual(jogo)).isEqualTo(primeira);
		assertThat(contar("SELECT count(*) FROM jogo_temporada WHERE jogo_id = ?")).isEqualTo(1);
	}

	@Test
	void chamadasSimultaneasIniciamUmaSoTemporada() throws Exception {
		CountDownLatch largada = new CountDownLatch(1);
		Callable<Boolean> iniciar = () -> {
			largada.await();
			try {
				temporadas.iniciarPrimeiraTemporada(jogo, 5);
				return true;
			}
			catch (IllegalStateException e) {
				return false;
			}
		};
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			List<Future<Boolean>> resultados = new ArrayList<>(List.of(executor.submit(iniciar), executor.submit(iniciar)));
			largada.countDown();
			List<Boolean> sucessos = new ArrayList<>();
			for (Future<Boolean> f : resultados) {
				sucessos.add(f.get());
			}
			assertThat(sucessos).containsExactlyInAnyOrder(true, false);
		}
		finally {
			executor.shutdownNow();
		}
		assertThat(contar("SELECT count(*) FROM jogo_temporada WHERE jogo_id = ?")).isEqualTo(1);
	}

	@Test
	void jogoInexistenteOuSemTemporadaERecusado() {
		assertThatThrownBy(() -> temporadas.iniciarPrimeiraTemporada(-1, 1)).isInstanceOf(NoSuchElementException.class);
		assertThatThrownBy(() -> temporadas.carregarAtual(jogo)).isInstanceOf(NoSuchElementException.class);
	}

	@Test
	void apagarJogoApagaATemporada() {
		temporadas.iniciarPrimeiraTemporada(jogo, 1);
		long temporada = contar("SELECT id FROM jogo_temporada WHERE jogo_id = ?");

		jdbc.sql("DELETE FROM jogo WHERE id = ?").param(jogo).update();

		assertThat(contar("SELECT count(*) FROM jogo_temporada WHERE jogo_id = ?")).isZero();
		for (String tabela : List.of("jogo_partida", "jogo_temporada_divisao", "jogo_bonus_artilharia")) {
			assertThat(jdbc.sql("SELECT count(*) FROM " + tabela + " WHERE temporada_id = ?").param(temporada)
				.query(Long.class).single()).as(tabela).isZero();
		}
	}

	@Test
	void clubeDoUsuarioComMediaAbaixoDe40TemOBeneficioENuncaEPrejudicado() {
		long clube = usuarioNoClubeComJogadores(20);

		Temporada iniciada = temporadas.iniciarPrimeiraTemporada(jogo, 2026);

		Temporada.SorteiosDivisao serieA = iniciada.divisoes().get(0);
		assertThat(serieA.sorte().beneficiados()).anyMatch(c -> c.id() == clube);
		assertThat(serieA.sorte().prejudicados()).hasSize(1).noneMatch(c -> c.id() == clube);
		assertThat(iniciada.divisoes().get(1).sorte().beneficiados()).hasSize(2);
		assertThat(iniciada.divisoes().get(1).sorte().prejudicados()).hasSize(1);
		assertThat(contar("SELECT count(*) FROM jogo_temporada_beneficiado b JOIN jogo_temporada t ON t.id = b.temporada_id "
				+ "WHERE t.jogo_id = ? AND b.prejudicado")).isEqualTo(2);
		assertThat(temporadas.carregarAtual(jogo)).isEqualTo(iniciada);
	}

	@Test
	void clubeDoUsuarioComMedia40OuMaisSoEBeneficiadoQuandoSorteadoENuncaPrejudicado() {
		long clube = usuarioNoClubeComJogadores(45);

		int beneficiado = 0;
		for (long semente = 0; semente < 40; semente++) {
			Temporada t = Temporada.iniciar(1, jogos.carregarLiga(jogo), jogos.carregarPlanteis(jogo), semente, Set.of(clube));
			assertThat(t.divisoes().get(0).sorte().beneficiados()).hasSize(2);
			assertThat(t.divisoes().get(0).sorte().prejudicados()).noneMatch(c -> c.id() == clube);
			beneficiado += t.divisoes().get(0).sorte().beneficiados().stream().anyMatch(c -> c.id() == clube) ? 1 : 0;
		}
		assertThat(beneficiado).isLessThan(40);
	}

	@Test
	void usuarioQueAssumeOPrejudicadoTiraAPenalizacaoDeVez() {
		Temporada iniciada = temporadas.iniciarPrimeiraTemporada(jogo, 2026);
		long anterior = clubeDoUsuario();
		long prejudicado = iniciada.divisoes().get(0).sorte().prejudicados().iterator().next().id();
		long prejudicadoB = iniciada.divisoes().get(1).sorte().prejudicados().iterator().next().id();

		jdbc.sql("UPDATE jogo_treinador SET clube_id = ? WHERE jogo_id = ?").params(prejudicado, jogo).update();
		temporadas.garantirBonusTreinador(jogo);

		Temporada carregada = temporadas.carregarAtual(jogo);
		assertThat(carregada.divisoes().get(0).sorte().prejudicados()).isEmpty();
		assertThat(carregada.divisoes().get(0).sorte().beneficiados()).isEqualTo(iniciada.divisoes().get(0).sorte().beneficiados());
		assertThat(carregada.divisoes().get(1).sorte().prejudicados()).extracting(Clube::id).containsExactly(prejudicadoB);

		// o usuário sai: a penalização não volta
		jdbc.sql("UPDATE jogo_treinador SET clube_id = ? WHERE jogo_id = ?").params(anterior, jogo).update();
		temporadas.garantirBonusTreinador(jogo);
		assertThat(temporadas.carregarAtual(jogo).divisoes().get(0).sorte().prejudicados()).isEmpty();
	}

	@Test
	void usuarioQueAssumeOPrejudicadoDoSorteioAntigoTiraAPenalizacao() {
		temporadas.iniciarPrimeiraTemporada(jogo, 2026);
		long temporada = contar("SELECT id FROM jogo_temporada WHERE jogo_id = ?");
		long serieA = contar("SELECT id FROM jogo_divisao WHERE jogo_id = ? AND prioridade = 1");
		List<Long> clubesA = jdbc.sql("SELECT id FROM jogo_clube WHERE divisao_id = ? ORDER BY id").param(serieA)
			.query(Long.class).list();
		jdbc.sql("DELETE FROM jogo_temporada_beneficiado WHERE temporada_id = ? AND divisao_id = ?").params(temporada, serieA).update();
		jdbc.sql("INSERT INTO jogo_temporada_divisao (temporada_id, divisao_id, beneficiado_id, prejudicado_id) VALUES (?, ?, ?, ?)")
			.params(temporada, serieA, clubesA.get(0), clubesA.get(1)).update();

		jdbc.sql("UPDATE jogo_treinador SET clube_id = ? WHERE jogo_id = ?").params(clubesA.get(1), jogo).update();
		temporadas.garantirBonusTreinador(jogo);

		Temporada carregada = temporadas.carregarAtual(jogo);
		assertThat(carregada.divisoes().get(0).sorte().prejudicados()).isEmpty();
		assertThat(carregada.divisoes().get(0).sorte().beneficiados()).extracting(Clube::id).containsExactly(clubesA.get(0));
	}

	@Test
	void temporadaComecadaAntesDaRev108UsaOBeneficiadoAntigo() {
		temporadas.iniciarPrimeiraTemporada(jogo, 2026);
		long temporada = contar("SELECT id FROM jogo_temporada WHERE jogo_id = ?");
		long serieA = contar("SELECT id FROM jogo_divisao WHERE jogo_id = ? AND prioridade = 1");
		List<Long> clubesA = jdbc.sql("SELECT id FROM jogo_clube WHERE divisao_id = ? ORDER BY id").param(serieA)
			.query(Long.class).list();
		// como gravava a V6: um beneficiado e um prejudicado, sem a tabela nova
		jdbc.sql("DELETE FROM jogo_temporada_beneficiado WHERE temporada_id = ? AND divisao_id = ?").params(temporada, serieA).update();
		jdbc.sql("INSERT INTO jogo_temporada_divisao (temporada_id, divisao_id, beneficiado_id, prejudicado_id) VALUES (?, ?, ?, ?)")
			.params(temporada, serieA, clubesA.get(0), clubesA.get(1)).update();

		Temporada carregada = temporadas.carregarAtual(jogo);

		assertThat(carregada.divisoes().get(0).sorte().beneficiados()).extracting(Clube::id).containsExactly(clubesA.get(0));
		assertThat(carregada.divisoes().get(0).sorte().prejudicados()).extracting(Clube::id).containsExactly(clubesA.get(1));
		assertThat(carregada.divisoes().get(1).sorte().beneficiados()).hasSize(2);
	}

	/** Põe o usuário no 1º clube da Série A, que tem 23 jogadores, todos com a força dada. */
	private long usuarioNoClubeComJogadores(int forca) {
		long clube = jdbc.sql("""
				SELECT c.id FROM jogo_clube c JOIN jogo_divisao d ON d.id = c.divisao_id
				WHERE c.jogo_id = ? AND d.prioridade = 1 AND EXISTS (SELECT 1 FROM jogo_jogador j WHERE j.clube_id = c.id)
				ORDER BY c.id LIMIT 1""").param(jogo).query(Long.class).single();
		jdbc.sql("UPDATE jogo_treinador SET clube_id = ? WHERE jogo_id = ?").params(clube, jogo).update();
		jdbc.sql("UPDATE jogo_jogador SET forca = ? WHERE clube_id = ?").params(forca, clube).update();
		return clube;
	}

	private long clubeDoUsuario() {
		return contar("SELECT clube_id FROM jogo_treinador WHERE jogo_id = ?");
	}

	private long contar(String sql) {
		return jdbc.sql(sql).param(jogo).query(Long.class).single();
	}

	private static List<Clube> clubes(String prefixo, int quantidade, int forca) {
		return IntStream.rangeClosed(1, quantidade).mapToObj(i -> new Clube(0, prefixo + " " + i, forca)).toList();
	}

	/** 23 jogadores: 3 goleiros e 20 de linha, os 4 primeiros de linha com asterisco. */
	private static List<Jogador> plantel() {
		return IntStream.range(0, 23)
			.mapToObj(i -> new Jogador(0, "J" + i, i < 3 ? Posicao.GOLEIRO : Posicao.ATACANTE, 20, Temperamento.NORMAL,
					i >= 3 && i < 7, "BRA"))
			.toList();
	}
}
