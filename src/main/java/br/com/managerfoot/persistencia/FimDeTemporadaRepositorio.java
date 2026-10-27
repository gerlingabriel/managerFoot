package br.com.managerfoot.persistencia;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SplittableRandom;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.temporada.Classificacao;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Divisao;
import br.com.managerfoot.temporada.Temporada;
import br.com.managerfoot.temporada.TransicaoTemporada;

/**
 * Fim de temporada (SPEC §4.1, §8.1; Etapa 3c.4): promoção, rebaixamento e fila de espera pela
 * classificação final, amarelos zerados (suspensões continuam, §7.4) e início da temporada
 * seguinte. Moral e força dos jogadores não mudam aqui.
 */
@Repository
public class FimDeTemporadaRepositorio {

	private final JdbcClient jdbc;
	private final JogoRepositorio jogos;
	private final TemporadaRepositorio temporadas;
	private final RodadaRepositorio rodadas;

	public FimDeTemporadaRepositorio(JdbcClient jdbc, JogoRepositorio jogos, TemporadaRepositorio temporadas,
			RodadaRepositorio rodadas) {
		this.jdbc = jdbc;
		this.jogos = jogos;
		this.temporadas = temporadas;
		this.rodadas = rodadas;
	}

	/**
	 * Encerra a temporada atual e inicia a seguinte. A semente decide o sorteio da fila e os
	 * sorteios da nova temporada.
	 *
	 * @param temporadaAtual a temporada que quem pede espera encerrar; um pedido repetido é recusado
	 * @return a transição e a nova temporada
	 * @throws NoSuchElementException se o jogo não existe ou não tem temporada
	 * @throws IllegalStateException se a temporada não é a atual ou ainda tem rodadas a jogar
	 */
	@Transactional
	public NovaTemporada avancarTemporada(long jogoId, int temporadaAtual, long semente) {
		jogos.travar(jogoId);
		int numero = temporadas.carregarAtual(jogoId).numero();
		if (numero != temporadaAtual) {
			throw new IllegalStateException("a temporada atual do jogo " + jogoId + " é a " + numero + ", não a "
					+ temporadaAtual);
		}
		int jogadas = rodadas.rodadasProcessadas(jogoId);
		int total = rodadas.totalDeRodadas(jogoId);
		if (jogadas < total) {
			throw new IllegalStateException("a temporada " + numero + " do jogo " + jogoId + " ainda tem "
					+ (total - jogadas) + " rodada(s) a jogar");
		}

		Map<Integer, Classificacao> finais = rodadas.classificacao(jogoId);
		// gerador da fila separado do usado por Temporada.iniciar com a mesma semente
		TransicaoTemporada transicao = TransicaoTemporada.aplicar(jogos.carregarLiga(jogoId), finais,
				new SplittableRandom(~semente));
		gravarDivisoes(jogoId, transicao.proxima());
		jdbc.sql("""
				UPDATE jogo_jogador SET amarelos = 0
				WHERE amarelos > 0 AND clube_id IN (SELECT id FROM jogo_clube WHERE jogo_id = :jogo)""")
			.param("jogo", jogoId)
			.update();

		Temporada nova = Temporada.iniciar(numero + 1, transicao.proxima(), jogos.carregarPlanteis(jogoId), semente);
		temporadas.gravar(jogoId, nova);
		return new NovaTemporada(transicao, nova);
	}

	private void gravarDivisoes(long jogoId, ConfiguracaoLiga proxima) {
		for (Divisao divisao : proxima.divisoes()) {
			for (Clube clube : divisao.clubes()) {
				jdbc.sql("""
						UPDATE jogo_clube SET divisao_id =
						  (SELECT id FROM jogo_divisao WHERE jogo_id = :jogo AND prioridade = :prioridade)
						WHERE id = :clube AND jogo_id = :jogo""")
					.param("jogo", jogoId)
					.param("prioridade", divisao.prioridade())
					.param("clube", clube.id())
					.update();
			}
		}
		for (Clube clube : proxima.filaDeEspera()) {
			jdbc.sql("UPDATE jogo_clube SET divisao_id = NULL WHERE id = :clube AND jogo_id = :jogo")
				.param("clube", clube.id())
				.param("jogo", jogoId)
				.update();
		}
	}

	/** Resultado do fim de temporada. */
	public record NovaTemporada(TransicaoTemporada transicao, Temporada temporada) {
	}
}
