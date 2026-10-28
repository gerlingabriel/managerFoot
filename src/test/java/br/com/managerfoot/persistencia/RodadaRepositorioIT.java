package br.com.managerfoot.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import br.com.managerfoot.TestcontainersConfiguration;
import br.com.managerfoot.temporada.Classificacao;
import br.com.managerfoot.temporada.Disciplina;
import br.com.managerfoot.temporada.LinhaClassificacao;
import br.com.managerfoot.temporada.ProcessamentoRodada;

/** Migração V7 e {@link RodadaRepositorio} contra PostgreSQL real. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RodadaRepositorioIT {

	@Autowired
	RodadaRepositorio rodadas;

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
	void criaJogoComTemporada() {
		jogo = LigaDeTeste.criarJogo(moldes, jogos, contas);
		LigaDeTeste.escalarTodosPelaMaquina(jdbc, jogo);
		temporadas.iniciarPrimeiraTemporada(jogo, 2026);
	}

	@Test
	void rodadaGravaPlacaresEGolsIguaisAoProcessamentoDoDominio() {
		ProcessamentoRodada.Resultado esperado = ProcessamentoRodada.padrao()
			.jogar(temporadas.carregarAtual(jogo), 1, jogos.carregarPlanteis(jogo), Disciplina.vazia(), Map.of());

		RodadaRepositorio.RodadaJogada jogada = rodadas.avancar(jogo, 1);

		assertThat(jogada.resultado()).isEqualTo(esperado);
		assertThat(rodadas.rodadasProcessadas(jogo)).isEqualTo(1);
		assertThat(contar("SELECT count(*) FROM jogo_partida p JOIN jogo_temporada t ON t.id = p.temporada_id "
				+ "WHERE t.jogo_id = ? AND p.gols_mandante IS NOT NULL")).isEqualTo(8 + 7);
		assertThat(contar("SELECT count(*) FROM jogo_partida p JOIN jogo_temporada t ON t.id = p.temporada_id "
				+ "WHERE t.jogo_id = ? AND p.rodada = 1 AND p.gols_mandante IS NULL")).isZero();
		// gols gravados como eventos batem com os placares
		assertThat(contar("""
				SELECT count(*) FROM jogo_partida p JOIN jogo_temporada t ON t.id = p.temporada_id
				WHERE t.jogo_id = ? AND p.gols_mandante IS NOT NULL AND p.gols_mandante + p.gols_visitante <>
				  (SELECT count(*) FROM jogo_evento e WHERE e.partida_id = p.id AND e.tipo = 'GOL')""")).isZero();
		assertThat(contar("SELECT count(*) FROM jogo_clube WHERE jogo_id = ? AND moral IS NOT NULL"))
			.isEqualTo(16 + 14);
	}

	@Test
	void pedidoRepetidoOuForaDeOrdemERecusadoSemDuplicar() {
		rodadas.avancar(jogo, 1);
		long eventos = contarEventos();

		assertThatThrownBy(() -> rodadas.avancar(jogo, 1)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("a próxima é a 2");
		assertThatThrownBy(() -> rodadas.avancar(jogo, 3)).isInstanceOf(IllegalStateException.class);
		assertThat(contarEventos()).isEqualTo(eventos);
		assertThat(rodadas.rodadasProcessadas(jogo)).isEqualTo(1);
	}

	@Test
	void pedidosSimultaneosDaMesmaRodadaJogamUmaVez() throws Exception {
		CountDownLatch largada = new CountDownLatch(1);
		Callable<Boolean> avancar = () -> {
			largada.await();
			try {
				rodadas.avancar(jogo, 1);
				return true;
			}
			catch (IllegalStateException e) {
				return false;
			}
		};
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			List<Future<Boolean>> futuros = List.of(executor.submit(avancar), executor.submit(avancar));
			largada.countDown();
			List<Boolean> sucessos = new ArrayList<>();
			for (Future<Boolean> f : futuros) {
				sucessos.add(f.get());
			}
			assertThat(sucessos).containsExactlyInAnyOrder(true, false);
		}
		finally {
			executor.shutdownNow();
		}
		assertThat(rodadas.rodadasProcessadas(jogo)).isEqualTo(1);
	}

	@Test
	void temporadaInteiraFechaClassificacaoECartoes() {
		for (int rodada = 1; rodada <= 30; rodada++) {
			rodadas.avancar(jogo, rodada);
		}

		assertThatThrownBy(() -> rodadas.avancar(jogo, 31)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("30 rodadas");
		Map<Integer, Classificacao> classificacao = rodadas.classificacao(jogo);
		assertThat(classificacao.get(1).linhas()).hasSize(16).allMatch(l -> l.jogos() == 30);
		assertThat(classificacao.get(2).linhas()).hasSize(15).allMatch(l -> l.jogos() == 28);
		for (Classificacao c : classificacao.values()) {
			int golsPro = c.linhas().stream().mapToInt(LinhaClassificacao::golsPro).sum();
			int golsContra = c.linhas().stream().mapToInt(LinhaClassificacao::golsContra).sum();
			assertThat(golsPro).isEqualTo(golsContra).isPositive();
		}
		assertThat(contar("SELECT count(*) FROM jogo_evento e JOIN jogo_clube c ON c.id = e.clube_id "
				+ "WHERE c.jogo_id = ? AND e.tipo = 'CARTAO_AMARELO'")).isPositive();
		// amarelos acumulados nunca chegam ao limite: ao chegar, viram suspensão
		assertThat(contar("SELECT count(*) FROM jogo_jogador j JOIN jogo_clube c ON c.id = j.clube_id "
				+ "WHERE c.jogo_id = ? AND j.amarelos >= 5")).isZero();
		assertThat(contar("SELECT count(*) FROM jogo_jogador j JOIN jogo_clube c ON c.id = j.clube_id "
				+ "WHERE c.jogo_id = ? AND j.amarelos > 0")).isPositive();
	}

	@Test
	void classificacaoDaPrimeiraRodadaDaTresPontosAoVencedor() {
		RodadaRepositorio.RodadaJogada jogada = rodadas.avancar(jogo, 1);

		Classificacao primeira = rodadas.classificacao(jogo).get(1);
		ProcessamentoRodada.PartidaJogada partida = jogada.resultado().partidas().stream()
			.filter(p -> p.prioridade() == 1)
			.findFirst()
			.orElseThrow();
		int gm = partida.resultado().golsCasa();
		int gv = partida.resultado().golsFora();
		int esperados = gm > gv ? Classificacao.PONTOS_VITORIA : gm == gv ? Classificacao.PONTOS_EMPATE : 0;
		assertThat(primeira.linha(partida.jogo().mandante()).pontos()).isEqualTo(esperados);
	}

	private long contarEventos() {
		return contar("SELECT count(*) FROM jogo_evento e JOIN jogo_clube c ON c.id = e.clube_id WHERE c.jogo_id = ?");
	}

	private long contar(String sql) {
		return jdbc.sql(sql).param(jogo).query(Long.class).single();
	}
}
