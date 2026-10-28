package br.com.managerfoot.persistencia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;
import br.com.managerfoot.economia.Moeda;
import br.com.managerfoot.economia.PrecoJogador;
import br.com.managerfoot.temporada.Artilharia;
import br.com.managerfoot.temporada.Classificacao;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Divisao;
import br.com.managerfoot.temporada.Jogo;
import br.com.managerfoot.temporada.LinhaClassificacao;
import br.com.managerfoot.temporada.Rodada;
import br.com.managerfoot.temporada.Temporada;

/**
 * Consultas das abas do treinador (SPEC §10.3, rev. 16): classificação por divisão, artilheiros da
 * temporada e de todos os tempos, e a ficha do jogador. Só leitura; o usuário precisa ser treinador
 * do jogo.
 */
@Repository
public class ConsultasRepositorio {

	/** Linhas mostradas nos rankings de artilheiros. */
	private static final int LINHAS_ARTILHEIROS = 20;

	private final JdbcClient jdbc;
	private final JogoRepositorio jogos;
	private final RodadaRepositorio rodadas;
	private final TemporadaRepositorio temporadas;

	public ConsultasRepositorio(JdbcClient jdbc, JogoRepositorio jogos, RodadaRepositorio rodadas,
			TemporadaRepositorio temporadas) {
		this.jdbc = jdbc;
		this.jogos = jogos;
		this.rodadas = rodadas;
		this.temporadas = temporadas;
	}

	/**
	 * Classificação da divisão (a do clube do usuário, se {@code prioridade} for nula).
	 *
	 * @throws NoSuchElementException se o usuário não é treinador do jogo
	 */
	@Transactional(readOnly = true)
	public VisaoClassificacao classificacao(long jogoId, long usuarioId, Integer prioridade) {
		long clube = exigirClube(jogoId, usuarioId);
		ConfiguracaoLiga liga = jogos.carregarLiga(jogoId);
		Divisao divisao = escolher(liga, clube, prioridade);
		Classificacao classificacao = rodadas.classificacao(jogoId).get(divisao.prioridade());
		return new VisaoClassificacao(abas(liga), divisao.prioridade(), divisao.nome(), rodadas.rodadasProcessadas(jogoId),
				classificacao.linhas(), divisao.rebaixados(), clube);
	}

	/** Artilheiros da temporada atual entre os jogadores dos clubes da divisão (a do usuário, se nula). */
	@Transactional(readOnly = true)
	public VisaoArtilheiros artilheirosDaTemporada(long jogoId, long usuarioId, Integer prioridade) {
		long clube = exigirClube(jogoId, usuarioId);
		ConfiguracaoLiga liga = jogos.carregarLiga(jogoId);
		Divisao divisao = escolher(liga, clube, prioridade);
		List<Long> clubes = divisao.clubes().stream().map(Clube::id).toList();
		Map<Long, Integer> gols = gols(jogoId, true);
		gols.keySet().removeIf(id -> !clubes.contains(clubeDoJogador(id)));
		return new VisaoArtilheiros(abas(liga), divisao.prioridade(), divisao.nome(), temporada(jogoId),
				linhas(new Artilharia(gols, Map.of()).rankingTemporada()), clube);
	}

	/** Artilheiros de todos os tempos do jogo salvo, pelos gols na carreira. */
	@Transactional(readOnly = true)
	public VisaoArtilheiros artilheirosDeTodosOsTempos(long jogoId, long usuarioId) {
		long clube = exigirClube(jogoId, usuarioId);
		return new VisaoArtilheiros(abas(jogos.carregarLiga(jogoId)), null, null, temporada(jogoId),
				linhas(new Artilharia(Map.of(), gols(jogoId, false)).rankingCarreira()), clube);
	}

	/** Ficha de um jogador do jogo (SPEC §10.3): gols na temporada e na carreira, força e amarelos. */
	@Transactional(readOnly = true)
	public Optional<Ficha> ficha(long jogoId, long usuarioId, long jogadorId) {
		long doUsuario = exigirClube(jogoId, usuarioId);
		Optional<Long> clube = jdbc.sql("""
				SELECT j.clube_id FROM jogo_jogador j JOIN jogo_clube c ON c.id = j.clube_id
				WHERE j.id = :jogador AND c.jogo_id = :jogo""")
			.param("jogador", jogadorId).param("jogo", jogoId).query(Long.class).optional();
		if (clube.isEmpty()) {
			return Optional.empty();
		}
		Jogador jogador = jogos.carregarPlanteis(jogoId).get(clube.get()).stream().filter(j -> j.id() == jogadorId)
			.findFirst().orElseThrow();
		String nomeClube = jdbc.sql("SELECT nome FROM jogo_clube WHERE id = :c").param("c", clube.get())
			.query(String.class).single();
		int amarelos = jdbc.sql("SELECT amarelos FROM jogo_jogador WHERE id = :j").param("j", jogadorId)
			.query(Integer.class).single();
		String pais = jdbc.sql("SELECT pais_codigo FROM jogo WHERE id = :jogo").param("jogo", jogoId).query(String.class).single();
		return Optional.of(new Ficha(jogador, nomeClube, gols(jogoId, true).getOrDefault(jogadorId, 0),
				gols(jogoId, false).getOrDefault(jogadorId, 0), amarelos, clube.get() == doUsuario, Moeda.doPais(pais),
				PrecoJogador.padrao().preco(jogador, pais)));
	}

	/**
	 * Calendário do clube na temporada atual (SPEC §10.3, rev. 17 e 18; modelo 3b, rev. 36): os jogos
	 * da liga, com o resultado dos já jogados, sem detalhes dos gols. Vazio se o clube não joga a
	 * temporada (fila de espera) ou não é deste jogo.
	 */
	@Transactional(readOnly = true)
	public Optional<VisaoCalendario> calendario(long jogoId, long usuarioId, long clubeId) {
		exigirClube(jogoId, usuarioId);
		Temporada temporada = temporadas.carregarAtual(jogoId);
		Optional<Temporada.SorteiosDivisao> divisao = temporada.divisoes().stream()
			.filter(d -> d.calendario().jogos().stream()
				.anyMatch(j -> j.mandante().id() == clubeId || j.visitante().id() == clubeId))
			.findFirst();
		if (divisao.isEmpty()) {
			return Optional.empty();
		}
		Map<Integer, int[]> placares = new HashMap<>();
		jdbc.sql("""
				SELECT p.rodada, p.mandante_id, p.gols_mandante, p.gols_visitante FROM jogo_partida p
				JOIN jogo_temporada t ON t.id = p.temporada_id
				WHERE t.jogo_id = :jogo AND t.numero = :numero AND p.gols_mandante IS NOT NULL
				  AND (p.mandante_id = :clube OR p.visitante_id = :clube)""")
			.param("jogo", jogoId)
			.param("numero", temporada.numero())
			.param("clube", clubeId)
			.query(rs -> {
				boolean emCasa = rs.getLong("mandante_id") == clubeId;
				int mandante = rs.getInt("gols_mandante");
				int visitante = rs.getInt("gols_visitante");
				placares.put(rs.getInt("rodada"), emCasa ? new int[] { mandante, visitante } : new int[] { visitante, mandante });
			});
		int proxima = rodadas.rodadasProcessadas(jogoId) + 1;
		List<JogoNoCalendario> jogosDoClube = new ArrayList<>();
		for (Rodada rodada : divisao.get().calendario().rodadas()) {
			Optional<Jogo> jogo = rodada.jogos().stream()
				.filter(j -> j.mandante().id() == clubeId || j.visitante().id() == clubeId).findFirst();
			int[] placar = placares.get(rodada.numero());
			jogosDoClube.add(new JogoNoCalendario(rodada.numero(), jogo.map(j -> j.mandante().id() == clubeId).orElse(null),
					jogo.map(j -> j.mandante().id() == clubeId ? j.visitante() : j.mandante()).orElse(null),
					placar == null ? null : placar[0], placar == null ? null : placar[1], rodada.numero() == proxima));
		}
		Classificacao classificacao = rodadas.classificacao(jogoId).get(divisao.get().prioridade());
		LinhaClassificacao linha = classificacao.linhas().stream().filter(l -> l.clube().id() == clubeId).findFirst()
			.orElseThrow();
		String nomeDivisao = jdbc.sql("SELECT nome FROM jogo_divisao WHERE jogo_id = :jogo AND prioridade = :p")
			.param("jogo", jogoId).param("p", divisao.get().prioridade()).query(String.class).single();
		return Optional.of(new VisaoCalendario(linha.clube(), jogos.ano(jogoId, temporada.numero()), nomeDivisao,
				classificacao.linhas().indexOf(linha) + 1, linha, jogosDoClube));
	}

	private List<LinhaArtilheiro> linhas(List<Artilharia.Artilheiro> ranking) {
		List<LinhaArtilheiro> linhas = new ArrayList<>();
		for (Artilharia.Artilheiro a : ranking.subList(0, Math.min(LINHAS_ARTILHEIROS, ranking.size()))) {
			jdbc.sql("""
					SELECT j.id, j.nome, j.posicao, c.id AS clube_id, c.nome AS clube
					FROM jogo_jogador j JOIN jogo_clube c ON c.id = j.clube_id WHERE j.id = :j""")
				.param("j", a.jogadorId())
				.query(rs -> {
					linhas.add(new LinhaArtilheiro(a.posicao(), a.jogadorId(), rs.getString("nome"),
							Posicao.valueOf(rs.getString("posicao")), rs.getLong("clube_id"),
							rs.getString("clube"), a.gols()));
				});
		}
		return linhas;
	}

	/** Gols por jogador: na temporada atual, ou em todas (carreira, desde o início do jogo salvo). */
	private Map<Long, Integer> gols(long jogoId, boolean soTemporadaAtual) {
		Map<Long, Integer> gols = new HashMap<>();
		jdbc.sql("""
				SELECT e.jogador_id, count(*) AS gols FROM jogo_evento e
				JOIN jogo_partida p ON p.id = e.partida_id JOIN jogo_temporada t ON t.id = p.temporada_id
				WHERE t.jogo_id = :jogo AND e.tipo = 'GOL'"""
				+ (soTemporadaAtual ? " AND t.numero = (SELECT max(numero) FROM jogo_temporada WHERE jogo_id = :jogo)" : "")
				+ " GROUP BY e.jogador_id")
			.param("jogo", jogoId)
			.query(rs -> {
				gols.put(rs.getLong("jogador_id"), rs.getInt("gols"));
			});
		return gols;
	}

	private long clubeDoJogador(long jogadorId) {
		return jdbc.sql("SELECT clube_id FROM jogo_jogador WHERE id = :j").param("j", jogadorId).query(Long.class).single();
	}

	/** Ano da temporada atual (SPEC §8.1.1, rev. 40). */
	private int temporada(long jogoId) {
		return jdbc.sql("""
				SELECT j.ano_inicial + max(t.numero) - 1 FROM jogo j JOIN jogo_temporada t ON t.jogo_id = j.id
				WHERE j.id = :jogo GROUP BY j.ano_inicial""").param("jogo", jogoId)
			.query(Integer.class).single();
	}

	/** A divisão pedida, ou a do clube do usuário (a última, se ele está na fila). */
	private static Divisao escolher(ConfiguracaoLiga liga, long clube, Integer prioridade) {
		if (prioridade != null) {
			return liga.divisoes().stream().filter(d -> d.prioridade() == prioridade).findFirst()
				.orElseThrow(() -> new NoSuchElementException("divisão " + prioridade + " não existe"));
		}
		return liga.divisoes().stream().filter(d -> d.clubes().stream().anyMatch(c -> c.id() == clube)).findFirst()
			.orElse(liga.divisoes().getLast());
	}

	private static List<AbaDivisao> abas(ConfiguracaoLiga liga) {
		return liga.divisoes().stream().map(d -> new AbaDivisao(d.prioridade(), d.nome())).toList();
	}

	private long exigirClube(long jogoId, long usuarioId) {
		return jogos.clubeDoTreinador(jogoId, usuarioId)
			.orElseThrow(() -> new NoSuchElementException("jogo " + jogoId + " não encontrado"));
	}

	public record AbaDivisao(int prioridade, String nome) {
	}

	/**
	 * @param rodadas rodadas já jogadas na temporada
	 * @param rebaixados quantos caem no fim, marcados na tabela
	 */
	public record VisaoClassificacao(List<AbaDivisao> divisoes, int prioridade, String divisao, int rodadas,
			List<LinhaClassificacao> linhas, int rebaixados, long clubeDoUsuario) {
	}

	public record LinhaArtilheiro(int posicao, long jogadorId, String nome, Posicao posicaoEmCampo,
			long clubeId, String clube, int gols) {
	}

	/**
	 * @param prioridade nula no ranking de todos os tempos
	 * @param temporada ano da temporada atual
	 */
	public record VisaoArtilheiros(List<AbaDivisao> divisoes, Integer prioridade, String divisao, int temporada,
			List<LinhaArtilheiro> linhas, long clubeDoUsuario) {
	}

	/**
	 * Um jogo no calendário do clube.
	 *
	 * @param emCasa nulo na folga
	 * @param adversario nulo na folga
	 * @param golsPro nulo enquanto o jogo não foi jogado
	 */
	public record JogoNoCalendario(int rodada, Boolean emCasa, Clube adversario, Integer golsPro, Integer golsContra,
			boolean proximo) {

		/** V, E ou D; nulo se ainda não foi jogado. */
		public String resultado() {
			if (golsPro == null) {
				return null;
			}
			return golsPro > golsContra ? "V" : golsPro.equals(golsContra) ? "E" : "D";
		}
	}

	/**
	 * @param temporada ano da temporada
	 * @param linha a linha do clube na classificação: jogos, vitórias, empates, derrotas e gols
	 */
	public record VisaoCalendario(Clube clube, int temporada, String divisao, int posicao, LinhaClassificacao linha,
			List<JogoNoCalendario> jogos) {
	}

	/**
	 * @param doUsuario o jogador é do clube do treinador: a ficha oferece a venda por leilão
	 * @param preco preço da compra direta pela tabela, em centavos de Real (referência para o lance mínimo)
	 */
	public record Ficha(Jogador jogador, String clube, int golsTemporada, int golsCarreira, int amarelos,
			boolean doUsuario, Moeda moeda, long preco) {
	}
}
