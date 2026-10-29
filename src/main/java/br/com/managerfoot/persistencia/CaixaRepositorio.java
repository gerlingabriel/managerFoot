package br.com.managerfoot.persistencia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.SplittableRandom;
import java.util.TreeMap;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.economia.CaixaDaRodada;
import br.com.managerfoot.economia.Lancamento;
import br.com.managerfoot.economia.MercadoDaMaquina;
import br.com.managerfoot.economia.Moeda;
import br.com.managerfoot.economia.Premiacao;
import br.com.managerfoot.economia.PublicoDoJogo;
import br.com.managerfoot.economia.TipoLancamento;
import br.com.managerfoot.temporada.Classificacao;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Divisao;
import br.com.managerfoot.temporada.Jogo;
import br.com.managerfoot.temporada.ProcessamentoRodada;
import br.com.managerfoot.temporada.Rodada;
import br.com.managerfoot.temporada.RodadaAoVivo;
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
	private final ClassificacaoRepositorio classificacoes;
	private final CaixaDaRodada caixa = CaixaDaRodada.padrao();
	private final PublicoDoJogo publico = PublicoDoJogo.padrao();
	private final Premiacao premiacao = Premiacao.padrao();

	public CaixaRepositorio(JdbcClient jdbc, JogoRepositorio jogos, TemporadaRepositorio temporadas,
			ClassificacaoRepositorio classificacoes) {
		this.jdbc = jdbc;
		this.jogos = jogos;
		this.temporadas = temporadas;
		this.classificacoes = classificacoes;
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
	 * Público de cada jogo da rodada ao vivo, pelo id do mandante (modelo 1a): o mesmo que o avanço
	 * vai gravar, porque usa a mesma semente, a mesma tabela (antes da rodada), os mesmos planteis e o
	 * estádio (o inicial, se o caixa ainda não foi aberto).
	 */
	@Transactional(readOnly = true)
	public Map<Long, Integer> publicos(long jogoId, List<RodadaAoVivo.PartidaDaRodada> partidas) {
		if (partidas.isEmpty()) {
			return Map.of();
		}
		return publicos(jogoId, temporadas.carregarAtual(jogoId), temporadaAtual(jogoId),
				partidas.stream().map(p -> new Partida(p.prioridade(), p.jogo(), p.semente())).toList(),
				jogos.carregarPlanteis(jogoId));
	}

	/**
	 * Público pela posição dos dois clubes na tabela antes da rodada, pela torcida do mandante (a força
	 * em campo) e, nas primeiras rodadas, pela posição final da temporada passada (SPEC §9.2, rev. 56 e
	 * 57). Sorteado pela semente da partida, com o complemento para não repetir a sequência dela.
	 */
	private Map<Long, Integer> publicos(long jogoId, Temporada temporada, long temporadaId, List<Partida> partidas,
			Map<Long, List<Jogador>> planteis) {
		int rodada = partidas.getFirst().jogo().rodada();
		Map<Integer, Classificacao> tabela = classificacoes.calcular(temporada, temporadaId, rodada);
		Map<Long, Integer> estadios = new HashMap<>();
		Map<Long, OptionalDouble> anteriores = new HashMap<>();
		jdbc.sql("""
				SELECT id, forca, estadio_capacidade, posicao_anterior, clubes_divisao_anterior
				FROM jogo_clube WHERE jogo_id = :jogo""")
			.param("jogo", jogoId)
			.query(rs -> {
				long id = rs.getLong("id");
				estadios.put(id, rs.getObject("estadio_capacidade") == null ? publico.estadioInicial(rs.getInt("forca"))
						: rs.getInt("estadio_capacidade"));
				anteriores.put(id, rs.getObject("posicao_anterior") == null ? OptionalDouble.empty()
						: OptionalDouble.of(PublicoDoJogo.relativa(rs.getInt("posicao_anterior"),
								rs.getInt("clubes_divisao_anterior"))));
			});
		Map<Long, Integer> publicos = new HashMap<>();
		for (Partida p : partidas) {
			Classificacao classificacao = tabela.get(p.prioridade());
			int rodadas = temporada.divisoes().stream().filter(d -> d.prioridade() == p.prioridade()).findFirst()
				.orElseThrow().calendario().rodadas().size();
			long mandante = p.jogo().mandante().id();
			long visitante = p.jogo().visitante().id();
			publicos.put(mandante, publico.publico(estadios.get(mandante),
					MercadoDaMaquina.forcaEmCampo(planteis.getOrDefault(mandante, List.of())), posicao(classificacao, mandante),
					posicao(classificacao, visitante), classificacao.linhas().size(), rodada, rodadas, anteriores.get(mandante),
					anteriores.get(visitante), new SplittableRandom(~p.semente())));
		}
		return publicos;
	}

	private static int posicao(Classificacao classificacao, long clubeId) {
		for (int i = 0; i < classificacao.linhas().size(); i++) {
			if (classificacao.linhas().get(i).clube().id() == clubeId) {
				return i + 1;
			}
		}
		throw new IllegalStateException("o clube " + clubeId + " não está na classificação da divisão");
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
				SELECT c.estadio_capacidade, c.forca, c.obra_rodadas, t.id AS temporada_id, t.rodadas_processadas,
				       d.nome AS divisao
				FROM jogo_clube c JOIN jogo_divisao d ON d.id = c.divisao_id
				JOIN jogo_temporada t ON t.jogo_id = c.jogo_id
				WHERE c.id = :clube ORDER BY t.numero DESC LIMIT 1""")
			.param("clube", clube)
			.query((rs, n) -> new Situacao(rs.getObject("estadio_capacidade") == null
					? publico.estadioInicial(rs.getInt("forca")) : rs.getInt("estadio_capacidade"),
					(Integer) rs.getObject("obra_rodadas"), rs.getLong("temporada_id"), rs.getInt("rodadas_processadas"),
					rs.getString("divisao")))
			.single();

		// modelo 6b (rev. 50): patrocínio (rodada 0) em linha própria; compras, vendas e prêmio na coluna
		// de mercado da rodada, com os detalhes
		Map<Integer, Map<TipoLancamento, Long>> porRodada = new TreeMap<>();
		Map<Integer, List<String>> detalhes = new HashMap<>();
		jdbc.sql("""
				SELECT l.rodada, l.tipo, l.valor, jj.nome, jj.posicao, jj.forca
				FROM jogo_lancamento l LEFT JOIN jogo_jogador jj ON jj.id = l.jogador_id
				WHERE l.clube_id = :clube AND l.temporada_id = :temporada
				ORDER BY l.rodada, l.id""")
			.param("clube", clube)
			.param("temporada", situacao.temporadaId())
			.query(rs -> {
				int rodada = rs.getInt("rodada");
				TipoLancamento tipo = TipoLancamento.valueOf(rs.getString("tipo"));
				porRodada.computeIfAbsent(rodada, r -> new HashMap<>()).merge(tipo, rs.getLong("valor"), Long::sum);
				String detalhe = switch (tipo) {
					case COMPRA, VENDA -> (tipo == TipoLancamento.COMPRA ? "Compra: " : "Venda: ") + rs.getString("nome")
							+ " (" + sigla(rs.getString("posicao")) + ", " + rs.getInt("forca") + ")";
					case PREMIO -> "Prêmio da liga";
					case ESTADIO -> "Ampliação do estádio";
					case PATROCINIO -> rodada == 0 ? null : "Patrocínio";
					default -> null;
				};
				if (detalhe != null) {
					detalhes.computeIfAbsent(rodada, r -> new ArrayList<>()).add(detalhe);
				}
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
		long saldoAnterior = saldo
				- porRodada.values().stream().flatMap(v -> v.values().stream()).mapToLong(Long::longValue).sum();
		long patrocinio = porRodada.getOrDefault(0, Map.of()).getOrDefault(TipoLancamento.PATROCINIO, 0L);
		List<LinhaExtrato> extrato = new ArrayList<>();
		long acumulado = saldoAnterior + patrocinio;
		for (Map.Entry<Integer, Map<TipoLancamento, Long>> e : porRodada.entrySet()) {
			int rodada = e.getKey();
			Map<TipoLancamento, Long> valores = e.getValue();
			long mercado = valores.getOrDefault(TipoLancamento.COMPRA, 0L) + valores.getOrDefault(TipoLancamento.VENDA, 0L)
					+ valores.getOrDefault(TipoLancamento.PREMIO, 0L) + valores.getOrDefault(TipoLancamento.ESTADIO, 0L)
					+ (rodada == 0 ? 0 : valores.getOrDefault(TipoLancamento.PATROCINIO, 0L));
			if (rodada == 0 && mercado == 0) {
				continue; // só o patrocínio, que tem linha própria
			}
			acumulado += valores.values().stream().mapToLong(Long::longValue).sum()
					- (rodada == 0 ? patrocinio : 0);
			Optional<Jogo> jogo = divisao.calendario().jogos().stream()
				.filter(j -> j.rodada() == rodada && envolve(j, clube)).findFirst();
			extrato.add(new LinhaExtrato(rodada, jogo.map(j -> j.mandante().id() == clube).orElse(null),
					jogo.map(j -> j.mandante().id() == clube ? j.visitante() : j.mandante()).orElse(null),
					publicos.get(rodada), valores.getOrDefault(TipoLancamento.BILHETERIA, 0L),
					valores.getOrDefault(TipoLancamento.CUSTO_DO_JOGO, 0L),
					valores.getOrDefault(TipoLancamento.DESLOCAMENTO, 0L), valores.getOrDefault(TipoLancamento.SALARIOS, 0L),
					mercado, detalhes.getOrDefault(rodada, List.of()), acumulado));
		}
		Integer publicoMedio = publicos.isEmpty() ? null
				: (int) Math.round(publicos.values().stream().mapToInt(Integer::intValue).average().orElseThrow());
		return new VisaoFinancas(oClube, jogos.ano(jogoId, temporada.numero()), temporada.numero() == 1, situacao.divisao(),
				situacao.rodadas(), moeda(jogoId), saldo, saldoAnterior, patrocinio, situacao.estadio(), situacao.obraRodadas(),
				publicoMedio,
				caixa.ingresso(divisao.prioridade()), folhas(jogoId).getOrDefault(clube, 0L), plantel.size(), extrato);
	}

	/**
	 * Lança o caixa da rodada jogada: público das partidas, extrato e saldo de cada clube das
	 * divisões. A fila de espera não joga e não tem lançamentos. Abre antes o caixa de quem ainda não
	 * tem.
	 *
	 * @param planteis os planteis que jogaram, antes da evolução de força
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void lancarRodada(long jogoId, long temporadaId, Temporada temporada, int rodada,
			List<ProcessamentoRodada.PartidaJogada> partidas, Map<Long, List<Jogador>> planteis) {
		abrirCaixas(jogoId, planteis);
		Map<Long, Integer> publicos = partidas.isEmpty() ? Map.of()
				: publicos(jogoId, temporada, temporadaId,
						partidas.stream().map(p -> new Partida(p.prioridade(), p.jogo(), p.semente())).toList(), planteis);
		Map<Long, Long> folhas = folhas(jogoId);
		for (Temporada.SorteiosDivisao divisao : temporada.divisoes()) {
			for (Rodada r : divisao.calendario().rodadas()) {
				if (r.numero() == rodada) {
					CaixaDaRodada.Movimento movimento = caixa.rodada(r, divisao.prioridade(), publicos, folhas);
					movimento.publicoPorMandante().forEach((mandante, pagantes) -> gravarPublico(temporadaId, rodada,
							mandante, pagantes));
					movimento.lancamentos().forEach((clube, lancamentos) -> lancar(temporadaId, rodada, clube, lancamentos));
				}
			}
		}
	}

	/**
	 * Patrocínio da temporada atual (SPEC §9.6, rev. 45 e 46), no início da temporada. Os clubes da
	 * máquina recebem a proposta fixa, de uma vez, na rodada 0; o clube de um usuário recebe a oferta
	 * com o mesmo valor de tabela e escolhe a proposta (modelo 9a, rev. 55). Na primeira temporada todos
	 * da divisão têm o mesmo valor; nas seguintes, pela posição na divisão em que o clube jogou; quem
	 * veio da fila conta como o último da divisão em que entra. Abre antes o caixa de quem ainda não tem.
	 *
	 * @param anterior classificação final da temporada anterior por prioridade da divisão; vazia na
	 *        primeira temporada
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void pagarPatrocinio(long jogoId, Map<Integer, Classificacao> anterior) {
		abrirCaixas(jogoId, jogos.carregarPlanteis(jogoId));
		long temporadaId = temporadaAtual(jogoId);
		ConfiguracaoLiga liga = jogos.carregarLiga(jogoId);
		Map<Integer, Integer> limites = new HashMap<>();
		liga.divisoes().forEach(d -> limites.put(d.prioridade(), d.limiteForca()));
		List<Long> doUsuario = jdbc.sql("SELECT clube_id FROM jogo_treinador WHERE jogo_id = :jogo")
			.param("jogo", jogoId)
			.query(Long.class)
			.list();
		for (Divisao divisao : liga.divisoes()) {
			int clubes = divisao.clubes().size();
			for (Clube clube : divisao.clubes()) {
				long valor = anterior.isEmpty() ? premiacao.patrocinioInicial(clubes, divisao.limiteForca())
						: anterior.entrySet().stream()
							.filter(e -> e.getValue().linhas().stream().anyMatch(l -> l.clube().id() == clube.id()))
							.findFirst()
							.map(e -> premiacao.patrocinio(e.getValue().posicao(clube), e.getValue().linhas().size(),
									limites.get(e.getKey())))
							.orElse(premiacao.patrocinio(clubes, clubes, divisao.limiteForca()));
				if (doUsuario.contains(clube.id())) {
					jdbc.sql("""
							INSERT INTO jogo_patrocinio (clube_id, temporada_id, tabela, jogos)
							VALUES (:clube, :temporada, :tabela, :jogos)""")
						.param("clube", clube.id())
						.param("temporada", temporadaId)
						.param("tabela", valor)
						.param("jogos", 2 * (clubes - 1))
						.update();
				}
				else {
					lancar(temporadaId, 0, clube.id(), List.of(new Lancamento(TipoLancamento.PATROCINIO, valor)));
				}
			}
		}
	}

	/**
	 * Pagamento do patrocínio do usuário (modelo 9a): a parte à vista na rodada 0, por vitória na rodada
	 * dela e o bônus de acesso na última. O bônus soma no lançamento da última rodada, se o clube também
	 * venceu nela.
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void lancarPatrocinio(long temporadaId, int rodada, long clubeId, long valor) {
		jdbc.sql("""
				INSERT INTO jogo_lancamento (clube_id, temporada_id, rodada, tipo, valor)
				VALUES (:clube, :temporada, :rodada, 'PATROCINIO', :valor)
				ON CONFLICT ON CONSTRAINT uq_jogo_lancamento DO UPDATE SET valor = jogo_lancamento.valor + EXCLUDED.valor""")
			.param("clube", clubeId)
			.param("temporada", temporadaId)
			.param("rodada", rodada)
			.param("valor", valor)
			.update();
		jdbc.sql("UPDATE jogo_clube SET saldo = saldo + :valor WHERE id = :clube")
			.param("valor", valor)
			.param("clube", clubeId)
			.update();
	}

	/**
	 * Prêmio da liga da temporada atual, que está terminando (SPEC §8.1, rev. 45 e 46): 1º, 2º e 3º de
	 * cada divisão, lançado na última rodada.
	 *
	 * @param finais classificação final por prioridade da divisão
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void pagarPremios(long jogoId, Map<Integer, Classificacao> finais) {
		long temporadaId = temporadaAtual(jogoId);
		int ultimaRodada = jdbc.sql("SELECT rodadas_processadas FROM jogo_temporada WHERE id = :temporada")
			.param("temporada", temporadaId)
			.query(Integer.class)
			.single();
		Map<Integer, Integer> limites = new HashMap<>();
		jogos.carregarLiga(jogoId).divisoes().forEach(d -> limites.put(d.prioridade(), d.limiteForca()));
		finais.forEach((prioridade, classificacao) -> {
			for (int posicao = 1; posicao <= classificacao.linhas().size(); posicao++) {
				long valor = premiacao.premio(posicao, limites.get(prioridade));
				if (valor > 0) {
					lancar(temporadaId, ultimaRodada, classificacao.linhas().get(posicao - 1).clube().id(),
							List.of(new Lancamento(TipoLancamento.PREMIO, valor)));
				}
			}
		});
	}

	private long temporadaAtual(long jogoId) {
		return jdbc.sql("SELECT id FROM jogo_temporada WHERE jogo_id = :jogo ORDER BY numero DESC LIMIT 1")
			.param("jogo", jogoId)
			.query(Long.class)
			.single();
	}

	/**
	 * Lançamento de compra ou venda de jogador na temporada atual, na última rodada processada, e o
	 * saldo do clube.
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void lancarMercado(long jogoId, long clubeId, TipoLancamento tipo, long valor, long jogadorId) {
		if (tipo != TipoLancamento.COMPRA && tipo != TipoLancamento.VENDA) {
			throw new IllegalArgumentException("lançamento de mercado deve ser compra ou venda, não " + tipo);
		}
		long temporadaId = temporadaAtual(jogoId);
		jdbc.sql("""
				INSERT INTO jogo_lancamento (clube_id, temporada_id, rodada, tipo, valor, jogador_id)
				SELECT :clube, t.id, t.rodadas_processadas, :tipo, :valor, :jogador FROM jogo_temporada t
				WHERE t.id = :temporada""")
			.param("clube", clubeId)
			.param("temporada", temporadaId)
			.param("tipo", tipo.name())
			.param("valor", valor)
			.param("jogador", jogadorId)
			.update();
		jdbc.sql("UPDATE jogo_clube SET saldo = saldo + :valor WHERE id = :clube")
			.param("valor", valor)
			.param("clube", clubeId)
			.update();
	}

	/**
	 * Folha de cada clube do jogo por rodada, pelos salários gravados (SPEC §9.5, rev. 58): a força do
	 * contrato nos clubes de usuário; a força atual nos da máquina, que readequam sempre. A faixa é a
	 * da força em campo atual do clube (rev. 66).
	 */
	@Transactional(readOnly = true)
	public Map<Long, Long> folhas(long jogoId) {
		Map<Long, List<int[]>> porClube = new HashMap<>();
		jdbc.sql("""
				SELECT j.clube_id, j.forca, coalesce(j.forca_salario, j.forca) AS forca_salario
				FROM jogo_jogador j JOIN jogo_clube c ON c.id = j.clube_id WHERE c.jogo_id = :jogo""")
			.param("jogo", jogoId)
			.query(rs -> {
				porClube.computeIfAbsent(rs.getLong("clube_id"), k -> new ArrayList<>())
					.add(new int[] { rs.getInt("forca"), rs.getInt("forca_salario") });
			});
		Map<Long, Long> folhas = new HashMap<>();
		porClube.forEach((clube, jogadores) -> {
			double emCampo = forcaEmCampo(jogadores.stream().mapToInt(j -> j[0]));
			folhas.put(clube, jogadores.stream().mapToLong(j -> caixa.salario(j[1], emCampo)).sum());
		});
		return folhas;
	}

	/** Força em campo atual do clube: média das 11 maiores forças (SPEC §4.3). */
	@Transactional(readOnly = true)
	public double forcaEmCampo(long clubeId) {
		return forcaEmCampo(jdbc.sql("SELECT forca FROM jogo_jogador WHERE clube_id = :clube").param("clube", clubeId)
			.query(Integer.class).list().stream().mapToInt(Integer::intValue));
	}

	private static double forcaEmCampo(java.util.stream.IntStream forcas) {
		return forcas.boxed().sorted(java.util.Comparator.reverseOrder()).limit(11).mapToInt(Integer::intValue).average()
			.orElse(0);
	}

	/** Pagamento da ampliação do estádio na rodada, e o saldo do clube. */
	@Transactional(propagation = Propagation.MANDATORY)
	public void lancarEstadio(long temporadaId, int rodada, long clubeId, long custo) {
		lancar(temporadaId, rodada, clubeId, List.of(new Lancamento(TipoLancamento.ESTADIO, -custo)));
	}

	/** Abre o caixa (estádio e saldo inicial) dos clubes do jogo que ainda não têm. */
	@Transactional(propagation = Propagation.MANDATORY)
	public void abrirCaixas(long jogoId) {
		abrirCaixas(jogoId, jogos.carregarPlanteis(jogoId));
	}

	/**
	 * Estádio inicial (75% da torcida, rev. 56) e saldo inicial dos clubes que ainda não têm caixa. Os
	 * caixas já abertos mantêm o estádio (rev. 57).
	 */
	private void abrirCaixas(long jogoId, Map<Long, List<Jogador>> planteis) {
		jdbc.sql("SELECT id, forca FROM jogo_clube WHERE jogo_id = :jogo AND saldo IS NULL")
			.param("jogo", jogoId)
			.query((rs, n) -> Map.entry(rs.getLong("id"), rs.getInt("forca")))
			.list()
			.forEach(c -> jdbc.sql("UPDATE jogo_clube SET estadio_capacidade = :capacidade, saldo = :saldo WHERE id = :clube")
				.param("capacidade", publico.estadioInicial(c.getValue()))
				.param("saldo", caixa.saldoInicial(planteis.getOrDefault(c.getKey(), List.of())))
				.param("clube", c.getKey())
				.update());
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

	private static String sigla(String posicao) {
		return switch (Posicao.valueOf(posicao)) {
			case GOLEIRO -> "G";
			case DEFENSOR -> "Z";
			case MEIO_CAMPISTA -> "M";
			case ATACANTE -> "A";
		};
	}

	private static boolean envolve(Jogo jogo, long clube) {
		return jogo.mandante().id() == clube || jogo.visitante().id() == clube;
	}

	private record Partida(int prioridade, Jogo jogo, long semente) {
	}

	private record Situacao(int estadio, Integer obraRodadas, long temporadaId, int rodadas, String divisao) {
	}

	/**
	 * Uma rodada do extrato, em centavos de Real: entradas positivas e saídas negativas.
	 *
	 * @param emCasa nulo na folga
	 * @param adversario nulo na folga
	 * @param publico nulo fora de casa e na folga
	 * @param saldo saldo depois da rodada
	 */
	/**
	 * Uma rodada do extrato; a rodada 0 só aparece com compras ou vendas feitas antes da 1ª rodada.
	 *
	 * @param mercado compras (negativas), vendas e prêmio da liga
	 * @param detalhes o que entrou em {@code mercado}, para mostrar embaixo do jogo
	 */
	public record LinhaExtrato(int rodada, Boolean emCasa, Clube adversario, Integer publico, long bilheteria,
			long custoDoJogo, long deslocamento, long salarios, long mercado, List<String> detalhes, long saldo) {

		public LinhaExtrato {
			detalhes = List.copyOf(detalhes);
		}

		public long resultado() {
			return bilheteria + custoDoJogo + deslocamento + salarios + mercado;
		}
	}

	/**
	 * A aba Finanças do clube do usuário, em centavos de Real.
	 *
	 * @param temporada ano da temporada
	 * @param rodadas rodadas já jogadas na temporada
	 * @param primeiraTemporada o saldo anterior é o saldo inicial do clube
	 * @param saldoAnterior saldo antes de qualquer lançamento da temporada (o do fim da anterior)
	 * @param patrocinio patrocínio da temporada, lançado no início
	 * @param estadio capacidade do estádio
	 * @param obraRodadas rodadas que faltam da obra de ampliação; nulo sem obra
	 * @param publicoMedio média dos jogos em casa na temporada; nulo se ainda não houve
	 * @param folha salários de uma rodada
	 */
	public record VisaoFinancas(Clube clube, int temporada, boolean primeiraTemporada, String divisao, int rodadas,
			Moeda moeda, long saldo, long saldoAnterior, long patrocinio, int estadio, Integer obraRodadas, Integer publicoMedio,
			long ingresso, long folha, int jogadores,
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

		public long totalMercado() {
			return extrato.stream().mapToLong(LinhaExtrato::mercado).sum();
		}

		/** Saídas da temporada, negativas. */
		public long despesas() {
			return totalCustoDoJogo() + totalDeslocamento() + totalSalarios();
		}

		/** Saldo no início da temporada, já com o patrocínio. */
		public long saldoInicioTemporada() {
			return saldoAnterior + patrocinio;
		}

		/** Resultado das rodadas: caixa, mercado e prêmio (o patrocínio está no início). */
		public long resultado() {
			return receitas() + despesas() + totalMercado();
		}

		/** Bilheteria média dos jogos em casa; nula sem jogo em casa. */
		public Long bilheteriaMedia() {
			long jogos = extrato.stream().filter(l -> Boolean.TRUE.equals(l.emCasa())).count();
			return jogos == 0 ? null : Math.round(receitas() / (double) jogos);
		}

		/** Ocupação média do estádio, em %; nula sem jogo em casa. */
		public Integer ocupacao() {
			return publicoMedio == null ? null : (int) Math.round(publicoMedio * 100.0 / estadio);
		}
	}
}
