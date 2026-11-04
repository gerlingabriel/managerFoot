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
import br.com.managerfoot.dominio.RegraEstrangeiros;
import br.com.managerfoot.economia.FiltroMercado;
import br.com.managerfoot.economia.Moeda;
import br.com.managerfoot.economia.PrecoJogador;

/**
 * Compra direta do treinador humano (SPEC §9.4, rev. 40 a 43): paga o preço da tabela ao clube da
 * máquina que anunciou o jogador. O servidor confere tudo; o contrato vale até o fim da temporada.
 */
@Repository
public class CompraRepositorio {

	private final JdbcClient jdbc;
	private final JogoRepositorio jogos;
	private final CaixaRepositorio caixas;
	private final TransferenciaRepositorio transferencias;
	private final PrecoJogador preco = PrecoJogador.padrao();

	public CompraRepositorio(JdbcClient jdbc, JogoRepositorio jogos, CaixaRepositorio caixas, TransferenciaRepositorio transferencias) {
		this.jdbc = jdbc;
		this.jogos = jogos;
		this.caixas = caixas;
		this.transferencias = transferencias;
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
		if (aoVivo(jogoId)) {
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

		List<Jogador> plantel = jogos.carregarPlantel(comprador);
		if (plantel.size() >= Clube.MAXIMO_JOGADORES) {
			throw new CompraRecusadaException("O plantel já tem " + Clube.MAXIMO_JOGADORES + " jogadores.");
		}
		Jogador jogador = jogos.carregarPlantel(vendedor).stream().filter(j -> j.id() == jogadorId).findFirst().orElseThrow();
		long valor = preco.preco(jogador, paisDaLiga(jogoId));
		caixas.abrirCaixas(jogoId);
		long saldo = caixas.saldo(comprador, plantel);
		if (saldo < valor) {
			throw new CompraRecusadaException("Caixa insuficiente para comprar " + jogador.nome() + ".");
		}

		transferencias.transferir(jogoId, jogadorId, vendedor, comprador, valor);
		return valor;
	}

	/**
	 * O plantel inteiro de um clube, de fora do campeonato inclusive, como o treinador vê ao clicar nele (modelo 7a,
	 * rev. 51): gols na temporada e, nos anunciados, o preço e se o treinador pode comprar.
	 *
	 * @throws NoSuchElementException se o usuário não treina clube no jogo ou o clube não é do jogo
	 */
	@Transactional(readOnly = true)
	public Vitrine vitrine(long jogoId, long usuarioId, long clubeId) {
		long doUsuario = jogos.clubeDoTreinador(jogoId, usuarioId)
			.orElseThrow(() -> new NoSuchElementException("jogo " + jogoId + " não encontrado"));
		boolean doJogo = jdbc.sql("SELECT EXISTS (SELECT 1 FROM jogo_clube WHERE id = :clube AND jogo_id = :jogo)")
			.param("clube", clubeId).param("jogo", jogoId).query(Boolean.class).single();
		if (!doJogo) {
			throw new NoSuchElementException("o clube " + clubeId + " não é do jogo " + jogoId);
		}
		List<Jogador> plantel = jogos.carregarPlantel(clubeId);
		List<Jogador> meu = jogos.carregarPlantel(doUsuario);
		// a tela mostra o preço durante a rodada, mas a compra só vale depois dela (modelo 14f, rev. 84)
		boolean aoVivo = aoVivo(jogoId);
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
					: aoVivo ? "Compra depois da rodada"
					: meu.size() >= Clube.MAXIMO_JOGADORES ? "Plantel cheio"
					: saldo < valor ? "Caixa insuficiente" : null;
			linhas.add(new JogadorNaVitrine(j, RegraEstrangeiros.deOutroPais(j.nacionalidade(), pais) ? j.nacionalidade() : null,
					RegraEstrangeiros.equiparado(j.nacionalidade(), pais), gols.getOrDefault(j.id(), 0), valor, impedimento));
		}
		return new Vitrine(clubeId == doUsuario, saldo, caixas.moeda(jogoId), linhas);
	}

	/**
	 * A aba "Comprar" (modelo 20b): os jogadores à venda dos clubes da máquina do jogo, das divisões e de
	 * fora delas, que passam nos filtros, na ordem pedida.
	 *
	 * @throws NoSuchElementException se o usuário não treina clube no jogo
	 */
	@Transactional(readOnly = true)
	public ListaDeCompra lista(long jogoId, long usuarioId, FiltroMercado filtro) {
		long doUsuario = jogos.clubeDoTreinador(jogoId, usuarioId)
			.orElseThrow(() -> new NoSuchElementException("jogo " + jogoId + " não encontrado"));
		List<Jogador> meu = jogos.carregarPlantel(doUsuario);
		boolean aoVivo = aoVivo(jogoId);
		long saldo = caixas.saldo(doUsuario, meu);
		String pais = paisDaLiga(jogoId);
		record Anuncio(long jogadorId, long clubeId, String clube, String onde, int gols) {
		}
		Map<Long, Anuncio> anuncios = new HashMap<>();
		jdbc.sql("""
				SELECT j.id, c.id AS clube_id, c.nome AS clube,
				       coalesce(d.nome, CASE WHEN c.pais_codigo IS NOT NULL AND c.pais_codigo <> jg.pais_codigo
				                             THEN c.pais_codigo ELSE 'fora da liga' END) AS onde,
				       (SELECT count(*) FROM jogo_evento e JOIN jogo_partida p ON p.id = e.partida_id
				        WHERE e.jogador_id = j.id AND e.tipo = 'GOL'
				          AND p.temporada_id = (SELECT max(t.id) FROM jogo_temporada t WHERE t.jogo_id = :jogo)) AS gols
				FROM jogo_jogador j
				JOIN jogo_clube c ON c.id = j.clube_id
				JOIN jogo jg ON jg.id = c.jogo_id
				LEFT JOIN jogo_divisao d ON d.id = c.divisao_id
				WHERE c.jogo_id = :jogo AND j.a_venda
				  AND NOT EXISTS (SELECT 1 FROM jogo_treinador t WHERE t.jogo_id = c.jogo_id AND t.clube_id = c.id)""")
			.param("jogo", jogoId)
			.query(rs -> {
				anuncios.put(rs.getLong("id"), new Anuncio(rs.getLong("id"), rs.getLong("clube_id"), rs.getString("clube"),
						rs.getString("onde"), rs.getInt("gols")));
			});
		Map<Long, Jogador> jogadores = new HashMap<>();
		anuncios.values().stream().map(Anuncio::clubeId).distinct()
			.forEach(clube -> jogos.carregarPlantel(clube).stream().filter(j -> anuncios.containsKey(j.id()))
				.forEach(j -> jogadores.put(j.id(), j)));
		List<LinhaCompra> linhas = jogadores.values().stream()
			.map(j -> new FiltroMercado.Oferta(j, preco.preco(j, pais), anuncios.get(j.id()).gols()))
			.filter(o -> filtro.aceita(o, saldo))
			.sorted(filtro.comparador())
			.map(o -> {
				Anuncio a = anuncios.get(o.jogador().id());
				String impedimento = aoVivo ? "Compra depois da rodada"
						: meu.size() >= Clube.MAXIMO_JOGADORES ? "Plantel cheio"
						: saldo < o.preco() ? "Caixa insuficiente" : null;
				return new LinhaCompra(o.jogador(), RegraEstrangeiros.deOutroPais(o.jogador().nacionalidade(), pais) ? o.jogador().nacionalidade() : null,
						RegraEstrangeiros.equiparado(o.jogador().nacionalidade(), pais), o.gols(), a.clubeId(), a.clube(), a.onde(),
						o.preco(), impedimento);
			})
			.toList();
		return new ListaDeCompra(saldo, caixas.moeda(jogoId), anuncios.size(), linhas);
	}

	/**
	 * @param aVenda jogadores à venda antes dos filtros
	 */
	public record ListaDeCompra(long saldo, Moeda moeda, int aVenda, List<LinhaCompra> jogadores) {

		public ListaDeCompra {
			jogadores = List.copyOf(jogadores);
		}
	}

	/**
	 * Um jogador na aba "Comprar".
	 *
	 * @param onde divisão do clube, sigla do país (clube de fora) ou "fora da liga"
	 * @param impedimento como em {@link JogadorNaVitrine}; nulo se pode comprar
	 */
	public record LinhaCompra(Jogador jogador, String estrangeiro, boolean equiparado, int gols, long clubeId, String clube,
			String onde, long preco, String impedimento) {
	}

	/** A moeda da liga do jogo. */
	public Moeda moeda(long jogoId) {
		return caixas.moeda(jogoId);
	}

	private boolean aoVivo(long jogoId) {
		return jdbc.sql("SELECT EXISTS (SELECT 1 FROM jogo_rodada_ao_vivo WHERE jogo_id = :jogo)")
			.param("jogo", jogoId)
			.query(Boolean.class)
			.single();
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
	 * @param equiparado é de outro país, mas não conta como estrangeiro na liga (SPEC §8.4)
	 * @param preco preço da compra direta; nulo se o jogador não está à venda
	 * @param impedimento por que o treinador não pode comprar ("Caixa insuficiente", "Plantel cheio",
	 *        "Compra depois da rodada");
	 *        nulo se pode ou se não está à venda
	 */
	public record JogadorNaVitrine(Jogador jogador, String estrangeiro, boolean equiparado, int gols, Long preco, String impedimento) {

		public boolean aVenda() {
			return preco != null;
		}
	}
}
