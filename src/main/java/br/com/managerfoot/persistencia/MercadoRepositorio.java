package br.com.managerfoot.persistencia;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.economia.MercadoDaMaquina;

/** Estado do mercado no jogo (SPEC §9.4, rev. 43): jogadores à venda para compra direta. */
@Repository
public class MercadoRepositorio {

	private final JdbcClient jdbc;
	private final JogoRepositorio jogos;
	private final MercadoDaMaquina mercado = MercadoDaMaquina.padrao();

	public MercadoRepositorio(JdbcClient jdbc, JogoRepositorio jogos) {
		this.jdbc = jdbc;
		this.jogos = jogos;
	}

	/**
	 * Completa os jogadores à venda dos clubes da máquina que estão nas divisões e retira os anúncios
	 * dos clubes de treinador humano (que só vendem por leilão, quando quiserem). Chamado no início de
	 * cada temporada e depois de cada rodada; quem chama abre a transação.
	 *
	 * @param semente sorteio reproduzível: a da temporada combinada com a rodada
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void reporAVenda(long jogoId, long semente) {
		jdbc.sql("""
				UPDATE jogo_jogador jj SET a_venda = false
				FROM jogo_treinador t
				WHERE t.jogo_id = :jogo AND t.clube_id = jj.clube_id AND jj.a_venda""")
			.param("jogo", jogoId)
			.update();
		List<Long> clubesDaMaquina = jdbc.sql("""
				SELECT c.id FROM jogo_clube c
				WHERE c.jogo_id = :jogo AND c.divisao_id IS NOT NULL
				  AND NOT EXISTS (SELECT 1 FROM jogo_treinador t WHERE t.jogo_id = c.jogo_id AND t.clube_id = c.id)
				ORDER BY c.id""")
			.param("jogo", jogoId)
			.query(Long.class)
			.list();
		Set<Long> aVenda = new HashSet<>(jdbc.sql("""
				SELECT jj.id FROM jogo_jogador jj JOIN jogo_clube c ON c.id = jj.clube_id
				WHERE c.jogo_id = :jogo AND jj.a_venda""")
			.param("jogo", jogoId)
			.query(Long.class)
			.list());
		Map<Long, List<Jogador>> planteis = jogos.carregarPlanteis(jogoId);
		SplittableRandom aleatorio = new SplittableRandom(semente);
		for (long clube : clubesDaMaquina) {
			List<Jogador> plantel = planteis.getOrDefault(clube, List.of());
			Set<Long> doClube = new HashSet<>();
			plantel.stream().map(Jogador::id).filter(aVenda::contains).forEach(doClube::add);
			for (Jogador novo : mercado.sortearAVenda(plantel, doClube, aleatorio)) {
				jdbc.sql("UPDATE jogo_jogador SET a_venda = true WHERE id = :id")
					.param("id", novo.id())
					.update();
			}
		}
	}

	/** Semente do sorteio dos anúncios depois da rodada (0 = início da temporada). */
	public static long semente(long sementeTemporada, int rodada) {
		return sementeTemporada * 31 + rodada;
	}

	/** Ids dos jogadores à venda do clube. */
	@Transactional(readOnly = true)
	public Set<Long> aVenda(long clubeId) {
		return new HashSet<>(jdbc.sql("SELECT id FROM jogo_jogador WHERE clube_id = :clube AND a_venda")
			.param("clube", clubeId)
			.query(Long.class)
			.list());
	}
}
