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
import br.com.managerfoot.temporada.Artilharia;
import br.com.managerfoot.temporada.Classificacao;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Divisao;
import br.com.managerfoot.temporada.LinhaClassificacao;

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

	public ConsultasRepositorio(JdbcClient jdbc, JogoRepositorio jogos, RodadaRepositorio rodadas) {
		this.jdbc = jdbc;
		this.jogos = jogos;
		this.rodadas = rodadas;
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
		exigirClube(jogoId, usuarioId);
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
		return Optional.of(new Ficha(jogador, nomeClube, gols(jogoId, true).getOrDefault(jogadorId, 0),
				gols(jogoId, false).getOrDefault(jogadorId, 0), amarelos));
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

	private int temporada(long jogoId) {
		return jdbc.sql("SELECT max(numero) FROM jogo_temporada WHERE jogo_id = :jogo").param("jogo", jogoId)
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

	/** @param prioridade nula no ranking de todos os tempos */
	public record VisaoArtilheiros(List<AbaDivisao> divisoes, Integer prioridade, String divisao, int temporada,
			List<LinhaArtilheiro> linhas, long clubeDoUsuario) {
	}

	public record Ficha(Jogador jogador, String clube, int golsTemporada, int golsCarreira, int amarelos) {
	}
}
