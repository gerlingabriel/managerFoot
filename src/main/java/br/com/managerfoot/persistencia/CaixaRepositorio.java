package br.com.managerfoot.persistencia;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Moral;
import br.com.managerfoot.economia.CaixaDaRodada;
import br.com.managerfoot.economia.Lancamento;
import br.com.managerfoot.temporada.ProcessamentoRodada;
import br.com.managerfoot.temporada.Rodada;
import br.com.managerfoot.temporada.Temporada;

/**
 * Caixa dos clubes do jogo (SPEC §9.1, rev. 37): saldo, estádio, público e extrato por rodada. Só
 * roda dentro do avanço da rodada, que já garante que cada rodada é processada uma vez; a chave do
 * extrato recusa um segundo lançamento da mesma rodada.
 */
@Repository
public class CaixaRepositorio {

	private final JdbcClient jdbc;
	private final CaixaDaRodada caixa = CaixaDaRodada.padrao();

	public CaixaRepositorio(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/**
	 * Lança o caixa da rodada jogada: público das partidas, extrato e saldo de cada clube das
	 * divisões. A fila de espera não joga e não tem lançamentos. Abre antes o caixa de quem ainda não
	 * tem.
	 *
	 * @param moralAntes moral dos clubes antes da rodada, que decide o público
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void lancarRodada(long jogoId, long temporadaId, Temporada temporada, int rodada,
			List<ProcessamentoRodada.PartidaJogada> partidas, Map<Long, Moral> moralAntes,
			Map<Long, List<Jogador>> planteis) {
		abrirCaixas(jogoId, planteis);
		Map<Long, Integer> capacidades = capacidades(jogoId);
		Map<Long, Long> sementes = new HashMap<>();
		partidas.forEach(p -> sementes.put(p.jogo().mandante().id(), p.semente()));
		for (Temporada.SorteiosDivisao divisao : temporada.divisoes()) {
			for (Rodada r : divisao.calendario().rodadas()) {
				if (r.numero() == rodada) {
					CaixaDaRodada.Movimento movimento = caixa.rodada(r, divisao.prioridade(), sementes, capacidades,
							moralAntes, planteis);
					movimento.publicoPorMandante().forEach((mandante, publico) -> gravarPublico(temporadaId, rodada,
							mandante, publico));
					movimento.lancamentos().forEach((clube, lancamentos) -> lancar(temporadaId, rodada, clube, lancamentos));
				}
			}
		}
	}

	/** Capacidade inicial do estádio e saldo inicial dos clubes que ainda não têm caixa. */
	private void abrirCaixas(long jogoId, Map<Long, List<Jogador>> planteis) {
		jdbc.sql("SELECT id, forca FROM jogo_clube WHERE jogo_id = :jogo AND saldo IS NULL")
			.param("jogo", jogoId)
			.query((rs, n) -> Map.entry(rs.getLong("id"), rs.getInt("forca")))
			.list()
			.forEach(c -> jdbc.sql("UPDATE jogo_clube SET estadio_capacidade = :capacidade, saldo = :saldo WHERE id = :clube")
				.param("capacidade", caixa.capacidadeInicial(c.getValue()))
				.param("saldo", caixa.saldoInicial(planteis.getOrDefault(c.getKey(), List.of())))
				.param("clube", c.getKey())
				.update());
	}

	private Map<Long, Integer> capacidades(long jogoId) {
		Map<Long, Integer> capacidades = new HashMap<>();
		jdbc.sql("SELECT id, estadio_capacidade FROM jogo_clube WHERE jogo_id = :jogo")
			.param("jogo", jogoId)
			.query((rs, n) -> Map.entry(rs.getLong("id"), rs.getInt("estadio_capacidade")))
			.list()
			.forEach(e -> capacidades.put(e.getKey(), e.getValue()));
		return capacidades;
	}

	private void gravarPublico(long temporadaId, int rodada, long mandanteId, int publico) {
		jdbc.sql("""
				UPDATE jogo_partida SET publico = :publico
				WHERE temporada_id = :temporada AND rodada = :rodada AND mandante_id = :mandante""")
			.param("publico", publico)
			.param("temporada", temporadaId)
			.param("rodada", rodada)
			.param("mandante", mandanteId)
			.update();
	}

	private void lancar(long temporadaId, int rodada, long clubeId, List<Lancamento> lancamentos) {
		for (Lancamento l : lancamentos) {
			jdbc.sql("""
					INSERT INTO jogo_lancamento (clube_id, temporada_id, rodada, tipo, valor)
					VALUES (:clube, :temporada, :rodada, :tipo, :valor)""")
				.param("clube", clubeId)
				.param("temporada", temporadaId)
				.param("rodada", rodada)
				.param("tipo", l.tipo().name())
				.param("valor", l.valor())
				.update();
		}
		jdbc.sql("UPDATE jogo_clube SET saldo = saldo + :total WHERE id = :clube")
			.param("total", lancamentos.stream().mapToLong(Lancamento::valor).sum())
			.param("clube", clubeId)
			.update();
	}
}
