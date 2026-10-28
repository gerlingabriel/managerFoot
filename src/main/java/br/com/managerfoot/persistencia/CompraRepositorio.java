package br.com.managerfoot.persistencia;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.economia.Moeda;
import br.com.managerfoot.economia.PrecoJogador;
import br.com.managerfoot.economia.TipoLancamento;

/**
 * Compra direta do treinador humano (SPEC §9.4, rev. 40 a 43): paga o preço da tabela ao clube da
 * máquina que anunciou o jogador. O servidor confere tudo; o contrato vale até o fim da temporada.
 */
@Repository
public class CompraRepositorio {

	private final JdbcClient jdbc;
	private final JogoRepositorio jogos;
	private final CaixaRepositorio caixas;
	private final PrecoJogador preco = PrecoJogador.padrao();

	public CompraRepositorio(JdbcClient jdbc, JogoRepositorio jogos, CaixaRepositorio caixas) {
		this.jdbc = jdbc;
		this.jogos = jogos;
		this.caixas = caixas;
	}

	/**
	 * Compra o jogador anunciado para o clube do usuário.
	 *
	 * @return o valor pago, em centavos de Real
	 * @throws NoSuchElementException se o usuário não treina clube no jogo ou o jogador não está à
	 *         venda num clube da máquina deste jogo
	 * @throws CompraRecusadaException se há rodada ao vivo, o plantel está cheio ou falta caixa
	 */
	@Transactional
	public long comprar(long jogoId, long usuarioId, long jogadorId) {
		jogos.travar(jogoId);
		long comprador = jogos.clubeDoTreinador(jogoId, usuarioId)
			.orElseThrow(() -> new NoSuchElementException("jogo " + jogoId + " não encontrado"));
		boolean aoVivo = jdbc.sql("SELECT EXISTS (SELECT 1 FROM jogo_rodada_ao_vivo WHERE jogo_id = :jogo)")
			.param("jogo", jogoId)
			.query(Boolean.class)
			.single();
		if (aoVivo) {
			throw new CompraRecusadaException("Não dá para comprar durante a rodada.");
		}
		long vendedor = jdbc.sql("""
				SELECT jj.clube_id FROM jogo_jogador jj JOIN jogo_clube c ON c.id = jj.clube_id
				WHERE jj.id = :jogador AND c.jogo_id = :jogo AND jj.a_venda
				  AND NOT EXISTS (SELECT 1 FROM jogo_treinador t WHERE t.jogo_id = c.jogo_id AND t.clube_id = c.id)""")
			.param("jogador", jogadorId)
			.param("jogo", jogoId)
			.query(Long.class)
			.optional()
			.orElseThrow(() -> new NoSuchElementException("o jogador " + jogadorId + " não está à venda"));

		var planteis = jogos.carregarPlanteis(jogoId);
		List<Jogador> plantel = planteis.getOrDefault(comprador, List.of());
		if (plantel.size() >= Clube.MAXIMO_JOGADORES) {
			throw new CompraRecusadaException("O plantel já tem " + Clube.MAXIMO_JOGADORES + " jogadores.");
		}
		Jogador jogador = planteis.get(vendedor).stream().filter(j -> j.id() == jogadorId).findFirst().orElseThrow();
		long valor = preco.preco(jogador, paisDaLiga(jogoId));
		caixas.abrirCaixas(jogoId);
		long saldo = caixas.saldo(comprador, plantel);
		if (saldo < valor) {
			throw new CompraRecusadaException("Caixa insuficiente para comprar " + jogador.nome() + ".");
		}

		// o jogador sai da escalação do vendedor antes de mudar de clube (chave composta clube + jogador)
		jdbc.sql("DELETE FROM jogo_escalacao_jogador WHERE clube_id = :clube AND jogador_id = :jogador")
			.param("clube", vendedor)
			.param("jogador", jogadorId)
			.update();
		jdbc.sql("UPDATE jogo_escalacao SET batedor_id = NULL WHERE clube_id = :clube AND batedor_id = :jogador")
			.param("clube", vendedor)
			.param("jogador", jogadorId)
			.update();
		jdbc.sql("UPDATE jogo_jogador SET clube_id = :comprador, a_venda = false WHERE id = :jogador")
			.param("comprador", comprador)
			.param("jogador", jogadorId)
			.update();
		caixas.lancarMercado(jogoId, comprador, TipoLancamento.COMPRA, -valor, jogadorId);
		caixas.lancarMercado(jogoId, vendedor, TipoLancamento.VENDA, valor, jogadorId);
		return valor;
	}

	/**
	 * O plantel inteiro de um clube como o treinador vê ao clicar nele na classificação (modelo 7a,
	 * rev. 51): gols na temporada e, nos anunciados, o preço e se o treinador pode comprar.
	 *
	 * @throws NoSuchElementException se o usuário não treina clube no jogo ou o clube não é do jogo
	 */
	@Transactional(readOnly = true)
	public Vitrine vitrine(long jogoId, long usuarioId, long clubeId) {
		long doUsuario = jogos.clubeDoTreinador(jogoId, usuarioId)
			.orElseThrow(() -> new NoSuchElementException("jogo " + jogoId + " não encontrado"));
		var planteis = jogos.carregarPlanteis(jogoId);
		List<Jogador> plantel = planteis.get(clubeId);
		if (plantel == null) {
			throw new NoSuchElementException("o clube " + clubeId + " não é do jogo " + jogoId);
		}
		List<Jogador> meu = planteis.getOrDefault(doUsuario, List.of());
		long saldo = caixas.saldo(doUsuario, meu);
		String pais = paisDaLiga(jogoId);
		Map<Long, Integer> gols = new HashMap<>();
		Set<Long> aVenda = new HashSet<>();
		jdbc.sql("""
				SELECT j.id, j.a_venda,
				       (SELECT count(*) FROM jogo_evento e JOIN jogo_partida p ON p.id = e.partida_id
				        WHERE e.jogador_id = j.id AND e.tipo = 'GOL'
				          AND p.temporada_id = (SELECT max(t.id) FROM jogo_temporada t WHERE t.jogo_id = :jogo)) AS gols
				FROM jogo_jogador j WHERE j.clube_id = :clube""")
			.param("jogo", jogoId)
			.param("clube", clubeId)
			.query(rs -> {
				gols.put(rs.getLong("id"), rs.getInt("gols"));
				if (rs.getBoolean("a_venda")) {
					aVenda.add(rs.getLong("id"));
				}
			});
		List<JogadorNaVitrine> linhas = new ArrayList<>();
		for (Jogador j : plantel.stream()
			.sorted(Comparator.comparing(Jogador::posicao).thenComparing(Comparator.comparingInt(Jogador::forca).reversed())
				.thenComparingLong(Jogador::id))
			.toList()) {
			boolean anunciado = clubeId != doUsuario && aVenda.contains(j.id());
			Long valor = anunciado ? preco.preco(j, pais) : null;
			String impedimento = !anunciado ? null
					: meu.size() >= Clube.MAXIMO_JOGADORES ? "Plantel cheio"
					: saldo < valor ? "Caixa insuficiente" : null;
			linhas.add(new JogadorNaVitrine(j, j.nacionalidade().equals(pais) ? null : j.nacionalidade(),
					gols.getOrDefault(j.id(), 0), valor, impedimento));
		}
		return new Vitrine(clubeId == doUsuario, saldo, caixas.moeda(jogoId), linhas);
	}

	private String paisDaLiga(long jogoId) {
		return jdbc.sql("SELECT pais_codigo FROM jogo WHERE id = :jogo")
			.param("jogo", jogoId)
			.query(String.class)
			.single();
	}

	/**
	 * Plantel de um clube na janela da classificação.
	 *
	 * @param doUsuario o clube é o do próprio treinador (nada a comprar)
	 * @param saldo caixa do clube do treinador
	 */
	public record Vitrine(boolean doUsuario, long saldo, Moeda moeda, List<JogadorNaVitrine> jogadores) {

		public Vitrine {
			jogadores = List.copyOf(jogadores);
		}

		public long aVenda() {
			return jogadores.stream().filter(JogadorNaVitrine::aVenda).count();
		}
	}

	/**
	 * @param estrangeiro sigla do país, se não é o da liga
	 * @param preco preço da compra direta; nulo se o jogador não está à venda
	 * @param impedimento por que o treinador não pode comprar ("Caixa insuficiente", "Plantel cheio");
	 *        nulo se pode ou se não está à venda
	 */
	public record JogadorNaVitrine(Jogador jogador, String estrangeiro, int gols, Long preco, String impedimento) {

		public boolean aVenda() {
			return preco != null;
		}
	}
}
