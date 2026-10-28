package br.com.managerfoot.persistencia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.TreeMap;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Moral;
import br.com.managerfoot.economia.CaixaDaRodada;
import br.com.managerfoot.economia.Lancamento;
import br.com.managerfoot.economia.Moeda;
import br.com.managerfoot.economia.TipoLancamento;
import br.com.managerfoot.temporada.Jogo;
import br.com.managerfoot.temporada.ProcessamentoRodada;
import br.com.managerfoot.temporada.Rodada;
import br.com.managerfoot.temporada.Temporada;

/**
 * Caixa dos clubes do jogo (SPEC §9.1, rev. 37): saldo, estádio, público e extrato por rodada. Os
 * lançamentos só rodam dentro do avanço da rodada, que já garante que cada rodada é processada uma
 * vez; a chave do extrato recusa um segundo lançamento da mesma rodada. A aba Finanças lê só o caixa
 * do clube do usuário (modelo 6a, rev. 39).
 */
@Repository
public class CaixaRepositorio {

	private final JdbcClient jdbc;
	private final JogoRepositorio jogos;
	private final TemporadaRepositorio temporadas;
	private final CaixaDaRodada caixa = CaixaDaRodada.padrao();

	public CaixaRepositorio(JdbcClient jdbc, JogoRepositorio jogos, TemporadaRepositorio temporadas) {
		this.jdbc = jdbc;
		this.jogos = jogos;
		this.temporadas = temporadas;
	}

	/**
	 * Saldo do clube. Antes do primeiro avanço o caixa ainda não foi aberto: vale o saldo inicial que
	 * o avanço vai gravar.
	 */
	@Transactional(readOnly = true)
	public long saldo(long clubeId, List<Jogador> plantel) {
		return jdbc.sql("SELECT saldo FROM jogo_clube WHERE id = :clube").param("clube", clubeId)
			.query((rs, n) -> rs.getObject("saldo") == null ? caixa.saldoInicial(plantel) : rs.getLong("saldo"))
			.single();
	}

	/** Moeda em que a liga do jogo mostra os valores (SPEC §9.1.1, rev. 37). */
	@Transactional(readOnly = true)
	public Moeda moeda(long jogoId) {
		return Moeda.doPais(jdbc.sql("SELECT pais_codigo FROM jogo WHERE id = :jogo").param("jogo", jogoId)
			.query(String.class).single());
	}

	/**
	 * Finanças do clube do usuário na temporada atual (modelo 6a, rev. 39): saldo, estádio, folha e o
	 * extrato por rodada. As finanças dos outros clubes não são visíveis.
	 *
	 * @throws NoSuchElementException se o usuário não é treinador do jogo
	 * @throws ClubeNaFilaException se o clube dele está na fila de espera
	 */
	@Transactional(readOnly = true)
	public VisaoFinancas financas(long jogoId, long usuarioId) {
		long clube = jogos.clubeDoTreinador(jogoId, usuarioId)
			.orElseThrow(() -> new NoSuchElementException("jogo " + jogoId + " não encontrado"));
		Temporada temporada = temporadas.carregarAtual(jogoId);
		Temporada.SorteiosDivisao divisao = temporada.divisoes().stream()
			.filter(d -> d.calendario().jogos().stream().anyMatch(j -> envolve(j, clube)))
			.findFirst()
			.orElseThrow(() -> new ClubeNaFilaException("o clube " + clube + " está na fila de espera"));
		Clube oClube = divisao.calendario().jogos().stream().filter(j -> envolve(j, clube))
			.map(j -> j.mandante().id() == clube ? j.mandante() : j.visitante()).findFirst().orElseThrow();
		List<Jogador> plantel = jogos.carregarPlanteis(jogoId).getOrDefault(clube, List.of());
		Situacao situacao = jdbc.sql("""
				SELECT c.estadio_capacidade, c.forca, t.id AS temporada_id, t.rodadas_processadas, d.nome AS divisao
				FROM jogo_clube c JOIN jogo_divisao d ON d.id = c.divisao_id
				JOIN jogo_temporada t ON t.jogo_id = c.jogo_id
				WHERE c.id = :clube ORDER BY t.numero DESC LIMIT 1""")
			.param("clube", clube)
			.query((rs, n) -> new Situacao(rs.getObject("estadio_capacidade") == null
					? caixa.capacidadeInicial(rs.getInt("forca")) : rs.getInt("estadio_capacidade"),
					rs.getLong("temporada_id"), rs.getInt("rodadas_processadas"), rs.getString("divisao")))
			.single();

		Map<Integer, Map<TipoLancamento, Long>> porRodada = new TreeMap<>();
		jdbc.sql("SELECT rodada, tipo, valor FROM jogo_lancamento WHERE clube_id = :clube AND temporada_id = :temporada")
			.param("clube", clube)
			.param("temporada", situacao.temporadaId())
			.query(rs -> {
				porRodada.computeIfAbsent(rs.getInt("rodada"), r -> new HashMap<>())
					.put(TipoLancamento.valueOf(rs.getString("tipo")), rs.getLong("valor"));
			});
		Map<Integer, Integer> publicos = new HashMap<>();
		jdbc.sql("""
				SELECT rodada, publico FROM jogo_partida
				WHERE temporada_id = :temporada AND mandante_id = :clube AND publico IS NOT NULL""")
			.param("temporada", situacao.temporadaId())
			.param("clube", clube)
			.query(rs -> {
				publicos.put(rs.getInt("rodada"), rs.getInt("publico"));
			});

		long saldo = saldo(clube, plantel);
		long saldoInicio = saldo - porRodada.values().stream().flatMap(v -> v.values().stream()).mapToLong(Long::longValue).sum();
		List<LinhaExtrato> extrato = new ArrayList<>();
		long acumulado = saldoInicio;
		for (Map.Entry<Integer, Map<TipoLancamento, Long>> e : porRodada.entrySet()) {
			int rodada = e.getKey();
			Map<TipoLancamento, Long> valores = e.getValue();
			acumulado += valores.values().stream().mapToLong(Long::longValue).sum();
			Optional<Jogo> jogo = divisao.calendario().jogos().stream()
				.filter(j -> j.rodada() == rodada && envolve(j, clube)).findFirst();
			extrato.add(new LinhaExtrato(rodada, jogo.map(j -> j.mandante().id() == clube).orElse(null),
					jogo.map(j -> j.mandante().id() == clube ? j.visitante() : j.mandante()).orElse(null),
					publicos.get(rodada), valores.getOrDefault(TipoLancamento.BILHETERIA, 0L),
					valores.getOrDefault(TipoLancamento.CUSTO_DO_JOGO, 0L),
					valores.getOrDefault(TipoLancamento.DESLOCAMENTO, 0L), valores.getOrDefault(TipoLancamento.SALARIOS, 0L),
					acumulado));
		}
		Integer publicoMedio = publicos.isEmpty() ? null
				: (int) Math.round(publicos.values().stream().mapToInt(Integer::intValue).average().orElseThrow());
		return new VisaoFinancas(oClube, temporada.numero(), situacao.divisao(), situacao.rodadas(), moeda(jogoId), saldo,
				saldoInicio, situacao.estadio(), publicoMedio, caixa.ingresso(divisao.prioridade()),
				caixa.folha(plantel), plantel.size(), extrato);
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

	private static boolean envolve(Jogo jogo, long clube) {
		return jogo.mandante().id() == clube || jogo.visitante().id() == clube;
	}

	private record Situacao(int estadio, long temporadaId, int rodadas, String divisao) {
	}

	/**
	 * Uma rodada do extrato, em centavos de Real: entradas positivas e saídas negativas.
	 *
	 * @param emCasa nulo na folga
	 * @param adversario nulo na folga
	 * @param publico nulo fora de casa e na folga
	 * @param saldo saldo depois da rodada
	 */
	public record LinhaExtrato(int rodada, Boolean emCasa, Clube adversario, Integer publico, long bilheteria,
			long custoDoJogo, long deslocamento, long salarios, long saldo) {

		public long resultado() {
			return bilheteria + custoDoJogo + deslocamento + salarios;
		}
	}

	/**
	 * A aba Finanças do clube do usuário, em centavos de Real.
	 *
	 * @param rodadas rodadas já jogadas na temporada
	 * @param saldoInicioTemporada saldo antes do primeiro lançamento da temporada
	 * @param estadio capacidade do estádio
	 * @param publicoMedio média dos jogos em casa na temporada; nulo se ainda não houve
	 * @param folha salários de uma rodada
	 */
	public record VisaoFinancas(Clube clube, int temporada, String divisao, int rodadas, Moeda moeda, long saldo,
			long saldoInicioTemporada, int estadio, Integer publicoMedio, long ingresso, long folha, int jogadores,
			List<LinhaExtrato> extrato) {

		public long receitas() {
			return extrato.stream().mapToLong(LinhaExtrato::bilheteria).sum();
		}

		public long totalCustoDoJogo() {
			return extrato.stream().mapToLong(LinhaExtrato::custoDoJogo).sum();
		}

		public long totalDeslocamento() {
			return extrato.stream().mapToLong(LinhaExtrato::deslocamento).sum();
		}

		public long totalSalarios() {
			return extrato.stream().mapToLong(LinhaExtrato::salarios).sum();
		}

		/** Saídas da temporada, negativas. */
		public long despesas() {
			return totalCustoDoJogo() + totalDeslocamento() + totalSalarios();
		}

		public long resultado() {
			return receitas() + despesas();
		}

		/** Ocupação média do estádio, em %; nula sem jogo em casa. */
		public Integer ocupacao() {
			return publicoMedio == null ? null : (int) Math.round(publicoMedio * 100.0 / estadio);
		}
	}
}
