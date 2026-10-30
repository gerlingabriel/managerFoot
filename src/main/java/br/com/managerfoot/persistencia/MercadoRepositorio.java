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

import br.com.managerfoot.bancopadrao.GeradorBancoPadrao;
import br.com.managerfoot.bancopadrao.ParametrosBancoPadrao;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.economia.MercadoDaMaquina;
import br.com.managerfoot.motor.ParametrosMotor;

/** Estado do mercado no jogo (SPEC §9.4, rev. 43): jogadores à venda para compra direta. */
@Repository
public class MercadoRepositorio {

	private final JdbcClient jdbc;
	private final JogoRepositorio jogos;
	private final MercadoDaMaquina mercado = MercadoDaMaquina.padrao();
	private final GeradorBancoPadrao gerador = new GeradorBancoPadrao(ParametrosBancoPadrao.padrao(), ParametrosMotor.padrao());

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

	/**
	 * Virada para os clubes de fora do campeonato (rev. 84): quem vendeu ganha jogador novo até voltar
	 * ao tamanho do plantel gerado, e o clube deixa uns dois à venda para a temporada. Só aqui: no meio
	 * da temporada, o vendido não é reposto. Quem chama abre a transação.
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void renovarClubesDeFora(long jogoId, long semente) {
		SplittableRandom aleatorio = new SplittableRandom(semente);
		List<ClubeDeFora> clubes = jdbc.sql("SELECT id, pais_codigo FROM jogo_clube WHERE jogo_id = :jogo AND pais_codigo IS NOT NULL ORDER BY id")
			.param("jogo", jogoId)
			.query((rs, n) -> new ClubeDeFora(rs.getLong("id"), rs.getString("pais_codigo")))
			.list();
		for (ClubeDeFora clube : clubes) {
			List<Jogador> plantel = jogos.carregarPlantel(clube.id());
			Set<String> nomes = new HashSet<>(plantel.stream().map(Jogador::nome).toList());
			for (MercadoDaMaquina.VagaDeFora vaga : mercado.reposicaoDeFora(plantel, ParametrosBancoPadrao.padrao().composicao())) {
				Jogador novo = gerador.jogadorDeReposicao(clube.pais(), vaga.posicao(), vaga.forca(), nomes, aleatorio);
				jdbc.sql("""
						INSERT INTO jogo_jogador (clube_id, nome, nacionalidade, posicao, forca, temperamento)
						VALUES (:clube, :nome, :nacionalidade, :posicao, :forca, :temperamento)""")
					.param("clube", clube.id())
					.param("nome", novo.nome())
					.param("nacionalidade", novo.nacionalidade())
					.param("posicao", novo.posicao().name())
					.param("forca", novo.forca())
					.param("temperamento", novo.temperamento().name())
					.update();
			}
			plantel = jogos.carregarPlantel(clube.id());
			for (Jogador j : mercado.sortearAVendaDeFora(plantel, aVenda(clube.id()), aleatorio)) {
				jdbc.sql("UPDATE jogo_jogador SET a_venda = true WHERE id = :id").param("id", j.id()).update();
			}
		}
	}

	private record ClubeDeFora(long id, String pais) {
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
