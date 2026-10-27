package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.SplittableRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Divisao;
import br.com.managerfoot.temporada.Temporada;
import br.com.managerfoot.temporada.TransicaoTemporada;

/** {@link FimDeTemporadaRepositorio} contra PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class FimDeTemporadaRepositorioIT {

	private static final long SEMENTE_NOVA = 77;

	@Autowired
	FimDeTemporadaRepositorio fim;

	@Autowired
	RodadaRepositorio rodadas;

	@Autowired
	TemporadaRepositorio temporadas;

	@Autowired
	JogoRepositorio jogos;

	@Autowired
	MoldeRepositorio moldes;

	@Autowired
	JdbcClient jdbc;

	long jogo;

	@BeforeEach
	void criaJogoComTemporada() {
		jogo = LigaDeTeste.criarJogo(moldes, jogos);
		temporadas.iniciarPrimeiraTemporada(jogo, 2026);
	}

	@Test
	void temporadaComRodadasAJogarNaoEncerra() {
		rodadas.avancar(jogo, 1);

		assertThatThrownBy(() -> fim.avancarTemporada(jogo, 1, SEMENTE_NOVA)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("29 rodada(s) a jogar");
		assertThat(temporadas.carregarAtual(jogo).numero()).isEqualTo(1);
	}

	@Test
	void fimDaTemporadaPromoveRebaixaZeraAmarelosEIniciaASeguinte() {
		for (int rodada = 1; rodada <= 30; rodada++) {
			rodadas.avancar(jogo, rodada);
		}
		ConfiguracaoLiga liga = jogos.carregarLiga(jogo);
		TransicaoTemporada esperada = TransicaoTemporada.aplicar(liga, rodadas.classificacao(jogo),
				new SplittableRandom(~SEMENTE_NOVA));
		long suspensoesAntes = contar("SELECT coalesce(sum(j.suspensao), 0) FROM jogo_jogador j "
				+ "JOIN jogo_clube c ON c.id = j.clube_id WHERE c.jogo_id = ?");
		assertThat(contar("SELECT count(*) FROM jogo_jogador j JOIN jogo_clube c ON c.id = j.clube_id "
				+ "WHERE c.jogo_id = ? AND j.amarelos > 0")).isPositive();

		FimDeTemporadaRepositorio.NovaTemporada nova = fim.avancarTemporada(jogo, 1, SEMENTE_NOVA);

		assertThat(nova.transicao()).isEqualTo(esperada);
		ConfiguracaoLiga proxima = jogos.carregarLiga(jogo);
		assertThat(nomes(proxima.divisoes().get(0))).containsExactlyInAnyOrderElementsOf(nomes(esperada.proxima().divisoes().get(0)));
		assertThat(nomes(proxima.divisoes().get(1))).containsExactlyInAnyOrderElementsOf(nomes(esperada.proxima().divisoes().get(1)));
		assertThat(proxima.filaDeEspera()).containsExactlyInAnyOrderElementsOf(esperada.semLiga());
		// os 2 da fila, que esperavam, subiram para a Série B
		assertThat(nomes(proxima.divisoes().get(1))).contains("Fila 1", "Fila 2");

		assertThat(contar("SELECT count(*) FROM jogo_jogador j JOIN jogo_clube c ON c.id = j.clube_id "
				+ "WHERE c.jogo_id = ? AND j.amarelos > 0")).isZero();
		assertThat(contar("SELECT coalesce(sum(j.suspensao), 0) FROM jogo_jogador j "
				+ "JOIN jogo_clube c ON c.id = j.clube_id WHERE c.jogo_id = ?")).isEqualTo(suspensoesAntes);

		Temporada segunda = temporadas.carregarAtual(jogo);
		assertThat(segunda).isEqualTo(nova.temporada());
		assertThat(segunda.numero()).isEqualTo(2);
		assertThat(rodadas.rodadasProcessadas(jogo)).isZero();
		assertThat(segunda.divisoes().get(1).calendario().jogos())
			.anyMatch(j -> j.mandante().nome().equals("Fila 1"));

		assertThatThrownBy(() -> fim.avancarTemporada(jogo, 1, SEMENTE_NOVA)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("é a 2");
		assertThat(rodadas.avancar(jogo, 1).numero()).isEqualTo(1);
	}

	private long contar(String sql) {
		return jdbc.sql(sql).param(jogo).query(Long.class).single();
	}

	private static List<String> nomes(Divisao divisao) {
		return divisao.clubes().stream().map(Clube::nome).toList();
	}
}
