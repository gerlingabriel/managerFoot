package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
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
		ConfiguracaoLiga liga = moldes.criarLiga(pais, new ConfiguracaoLiga("Nacional",
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

		assertThat(iniciada).isEqualTo(Temporada.iniciar(1, jogos.carregarLiga(jogo), jogos.carregarPlanteis(jogo), 2026));
		assertThat(temporadas.carregarAtual(jogo)).isEqualTo(iniciada);
		assertThat(iniciada.divisoes().get(1).calendario().rodadas()).allMatch(r -> r.folga().isPresent());
		assertThat(iniciada.divisoes())
			.allMatch(d -> d.bonus().asteriscoTemporario().size() + d.bonus().bonusExtra().size() == 15);
		assertThat(contar("SELECT count(*) FROM jogo_partida p JOIN jogo_temporada t ON t.id = p.temporada_id "
				+ "WHERE t.jogo_id = ?")).isEqualTo(16 * 15 + 17 * 16);
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
