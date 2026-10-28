package br.com.managerfoot.persistencia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.economia.Leilao.Lance;
import br.com.managerfoot.economia.LeiloesDaRodada;
import br.com.managerfoot.economia.LeiloesDaRodada.ClubeNoMercado;
import br.com.managerfoot.economia.LeiloesDaRodada.Desfecho;
import br.com.managerfoot.economia.LeiloesDaRodada.Oferta;
import br.com.managerfoot.economia.Moeda;
import br.com.managerfoot.temporada.ConfiguracaoLiga;
import br.com.managerfoot.temporada.Divisao;

/**
 * Leilões da máquina antes da rodada (SPEC §9.4, rev. 41 a 45; modelo 7b; docs/decisions/0012).
 * Abrem quando o treinador confirma a tática; ele dá os lances (se o caixa alcança algum lance
 * inicial) e depois a máquina dá os dela. Cada leilão é aberto e resolvido uma vez só.
 */
@Repository
public class LeilaoRepositorio {

	private final JdbcClient jdbc;
	private final JogoRepositorio jogos;
	private final CaixaRepositorio caixas;
	private final TransferenciaRepositorio transferencias;
	private final RodadaRepositorio rodadas;
	private final LeiloesDaRodada leiloes = LeiloesDaRodada.padrao();

	public LeilaoRepositorio(JdbcClient jdbc, JogoRepositorio jogos, CaixaRepositorio caixas, TransferenciaRepositorio transferencias,
			RodadaRepositorio rodadas) {
		this.jdbc = jdbc;
		this.jogos = jogos;
		this.caixas = caixas;
		this.transferencias = transferencias;
		this.rodadas = rodadas;
	}

	/** O que vem depois de confirmar a tática. */
	public enum Etapa {
		/** Sem leilão pendente: a rodada pode começar. */
		RODADA,
		/** Há leilões em que o treinador pode dar lance. */
		LANCES,
		/** Os leilões foram resolvidos agora, sem lance do treinador: mostrar o resultado. */
		RESULTADO
	}

	/**
	 * Abre os leilões da rodada, se ainda não foram abertos. Sem leilão em que o treinador possa dar
	 * lance, já resolve. Repetir o pedido não abre outros.
	 *
	 * @param rodada a rodada que o treinador vai jogar: a seguinte à última processada
	 * @throws NoSuchElementException se o usuário não treina clube no jogo
	 * @throws IllegalStateException se a rodada não é a próxima
	 */
	@Transactional
	public Etapa preparar(long jogoId, long usuarioId, int rodada) {
		long clube = clubeDoUsuario(jogoId, usuarioId);
		jogos.travar(jogoId);
		if (aoVivo(jogoId)) {
			return Etapa.RODADA;
		}
		LinhaTemporada t = temporadaAtual(jogoId);
		if (rodada != t.processadas() + 1 || rodada > rodadas.totalDeRodadas(jogoId)) {
			throw new IllegalStateException("a rodada " + rodada + " não é a próxima do jogo " + jogoId);
		}
		caixas.abrirCaixas(jogoId);
		Contexto c = contexto(jogoId);
		if (!existemLeiloes(t.id(), rodada)) {
			List<Oferta> ofertas = leiloes.abrir(c.daMaquina(), c.pais());
			for (Oferta o : ofertas) {
				jdbc.sql("""
						INSERT INTO jogo_leilao (temporada_id, rodada, jogador_id, vendedor_id, lance_minimo)
						VALUES (:temporada, :rodada, :jogador, :vendedor, :minimo)""")
					.param("temporada", t.id())
					.param("rodada", rodada)
					.param("jogador", o.jogador().id())
					.param("vendedor", o.vendedorId())
					.param("minimo", o.lanceMinimo())
					.update();
			}
		}
		List<LinhaLeilao> abertos = abertos(t.id(), rodada);
		if (abertos.isEmpty()) {
			return Etapa.RODADA;
		}
		ClubeNoMercado meu = c.humanos().stream().filter(h -> h.clube().id() == clube).findFirst().orElseThrow();
		if (abertos.stream().anyMatch(l -> impedimento(l.lanceMinimo(), meu, c.moeda()) == null)) {
			return Etapa.LANCES;
		}
		resolver(jogoId, t, rodada, c, abertos, Map.of());
		return Etapa.RESULTADO;
	}

	/**
	 * Lances do treinador, na moeda da liga (vazio = nenhum), e o resultado de todos os leilões
	 * abertos da rodada.
	 *
	 * @param lances valor inteiro na moeda da liga, por id do leilão
	 * @throws IllegalArgumentException se um lance não vale (abaixo do inicial, acima do caixa); nada
	 *         é gravado
	 * @throws IllegalStateException se não há leilão aberto antes desta rodada (já resolvidos)
	 */
	@Transactional
	public void darLances(long jogoId, long usuarioId, int rodada, Map<Long, Long> lances) {
		long clube = clubeDoUsuario(jogoId, usuarioId);
		jogos.travar(jogoId);
		LinhaTemporada t = temporadaAtual(jogoId);
		List<LinhaLeilao> abertos = rodada == t.processadas() + 1 && !aoVivo(jogoId) ? abertos(t.id(), rodada) : List.of();
		if (abertos.isEmpty()) {
			throw new IllegalStateException("não há leilão aberto antes da rodada " + rodada);
		}
		caixas.abrirCaixas(jogoId);
		Contexto c = contexto(jogoId);
		ClubeNoMercado meu = c.humanos().stream().filter(h -> h.clube().id() == clube).findFirst().orElseThrow();
		Map<Long, List<Lance>> porJogador = new HashMap<>();
		for (var e : lances.entrySet()) {
			LinhaLeilao leilao = abertos.stream().filter(l -> l.id() == e.getKey()).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Este leilão não está mais aberto."));
			String nome = c.jogador(leilao.jogadorId()).nome();
			if (impedimento(leilao.lanceMinimo(), meu, c.moeda()) != null) {
				throw new IllegalArgumentException("Você não pode dar lance por " + nome + ".");
			}
			long valor;
			try {
				valor = c.moeda().centavos(e.getValue());
			}
			catch (ArithmeticException ex) {
				throw new IllegalArgumentException("Lance por " + nome + " acima do seu caixa.");
			}
			if (valor <= 0 || valor < leilao.lanceMinimo()) {
				throw new IllegalArgumentException("Lance por " + nome + " abaixo do lance inicial ("
						+ c.moeda().formatar(c.moeda().centavos(c.moeda().unidadesAcima(leilao.lanceMinimo()))) + ").");
			}
			if (valor > meu.saldo()) {
				throw new IllegalArgumentException("Lance por " + nome + " acima do seu caixa.");
			}
			porJogador.put(leilao.jogadorId(), List.of(new Lance(clube, valor)));
		}
		resolver(jogoId, t, rodada, c, abertos, porJogador);
	}

	/**
	 * Os leilões antes da próxima rodada, como o treinador vê (modelo 7b): abertos para lance ou já
	 * resolvidos. Vazio se não houve leilão.
	 */
	@Transactional(readOnly = true)
	public Optional<VisaoLeiloes> visao(long jogoId, long usuarioId) {
		long clube = clubeDoUsuario(jogoId, usuarioId);
		LinhaTemporada t = temporadaAtual(jogoId);
		int rodada = t.processadas() + 1;
		List<LinhaLeilao> linhas = jdbc.sql("""
				SELECT id, jogador_id, vendedor_id, lance_minimo, situacao, comprador_id, valor, empate
				FROM jogo_leilao WHERE temporada_id = :temporada AND rodada = :rodada ORDER BY id""")
			.param("temporada", t.id())
			.param("rodada", rodada)
			.query((rs, n) -> new LinhaLeilao(rs.getLong("id"), rs.getLong("jogador_id"), rs.getLong("vendedor_id"),
					rs.getLong("lance_minimo"), Situacao.valueOf(rs.getString("situacao")),
					(Long) rs.getObject("comprador_id"), (Long) rs.getObject("valor"), rs.getBoolean("empate")))
			.list();
		if (linhas.isEmpty()) {
			return Optional.empty();
		}
		Contexto c = contexto(jogoId);
		ClubeNoMercado meu = c.humanos().stream().filter(h -> h.clube().id() == clube).findFirst().orElseThrow();
		Map<Long, Long> meusLances = new HashMap<>();
		jdbc.sql("""
				SELECT l.leilao_id, l.valor FROM jogo_leilao_lance l JOIN jogo_leilao j ON j.id = l.leilao_id
				WHERE j.temporada_id = :temporada AND j.rodada = :rodada AND l.clube_id = :clube""")
			.param("temporada", t.id())
			.param("rodada", rodada)
			.param("clube", clube)
			.query(rs -> {
				meusLances.put(rs.getLong("leilao_id"), rs.getLong("valor"));
			});
		List<LeilaoNaTela> naTela = new ArrayList<>();
		for (LinhaLeilao l : linhas) {
			Jogador j = c.jogador(l.jogadorId());
			naTela.add(new LeilaoNaTela(l.id(), j, j.nacionalidade().equals(c.pais()) ? null : j.nacionalidade(),
					c.clube(l.vendedorId()), l.lanceMinimo(), c.moeda().unidadesAcima(l.lanceMinimo()),
					l.situacao() == Situacao.ABERTO ? impedimento(l.lanceMinimo(), meu, c.moeda()) : null, l.situacao(),
					l.compradorId() == null ? null : c.clube(l.compradorId()), l.valor(), l.empate(),
					meusLances.get(l.id()), l.compradorId() != null && l.compradorId() == clube));
		}
		boolean aguardando = naTela.stream().anyMatch(l -> l.situacao() == Situacao.ABERTO);
		return Optional.of(new VisaoLeiloes(rodada, c.moeda(), meu.saldo(), aguardando, naTela));
	}

	/** Resolve os leilões abertos: o jogador que já saiu do vendedor (compra direta) é cancelado. */
	private void resolver(long jogoId, LinhaTemporada t, int rodada, Contexto c, List<LinhaLeilao> abertos,
			Map<Long, List<Lance>> lancesHumanos) {
		List<Oferta> ofertas = new ArrayList<>();
		Map<Long, Long> leilaoPorJogador = new HashMap<>();
		for (LinhaLeilao l : abertos) {
			Optional<Jogador> noVendedor = c.planteis().getOrDefault(l.vendedorId(), List.of()).stream()
				.filter(j -> j.id() == l.jogadorId()).findFirst();
			if (noVendedor.isEmpty()) {
				atualizar(l.id(), Situacao.CANCELADO, null, null, false);
				continue;
			}
			ofertas.add(new Oferta(noVendedor.get(), l.vendedorId(), l.lanceMinimo()));
			leilaoPorJogador.put(l.jogadorId(), l.id());
		}
		List<Desfecho> desfechos = leiloes.resolver(ofertas, c.daMaquina(), c.humanos(), lancesHumanos, c.pais(),
				new SplittableRandom(semente(t.semente(), rodada)));
		for (Desfecho d : desfechos) {
			long leilaoId = leilaoPorJogador.get(d.oferta().jogador().id());
			for (Lance lance : d.lances()) {
				jdbc.sql("INSERT INTO jogo_leilao_lance (leilao_id, clube_id, valor) VALUES (:leilao, :clube, :valor)")
					.param("leilao", leilaoId)
					.param("clube", lance.clubeId())
					.param("valor", lance.valor())
					.update();
			}
			if (d.vencedor().isPresent()) {
				Lance v = d.vencedor().get();
				transferencias.transferir(jogoId, d.oferta().jogador().id(), d.oferta().vendedorId(), v.clubeId(), v.valor());
				atualizar(leilaoId, Situacao.VENDIDO, v.clubeId(), v.valor(), d.empate());
			} else {
				atualizar(leilaoId, Situacao.SEM_LANCE, null, null, false);
			}
		}
	}

	/** Semente do sorteio dos empates nos leilões antes da rodada. */
	static long semente(long sementeTemporada, int rodada) {
		return sementeTemporada * 37 + rodada;
	}

	/** Por que o treinador não pode dar lance; nulo se pode. */
	private static String impedimento(long lanceMinimo, ClubeNoMercado meu, Moeda moeda) {
		if (meu.plantel().size() >= Clube.MAXIMO_JOGADORES) {
			return "Plantel cheio";
		}
		long menorLance = moeda.centavos(Math.max(1, moeda.unidadesAcima(lanceMinimo)));
		return meu.saldo() < menorLance ? "Caixa insuficiente" : null;
	}

	private void atualizar(long leilaoId, Situacao situacao, Long comprador, Long valor, boolean empate) {
		jdbc.sql("""
				UPDATE jogo_leilao SET situacao = :situacao, comprador_id = :comprador, valor = :valor, empate = :empate
				WHERE id = :id AND situacao = 'ABERTO'""")
			.param("situacao", situacao.name())
			.param("comprador", comprador)
			.param("valor", valor)
			.param("empate", empate)
			.param("id", leilaoId)
			.update();
	}

	/**
	 * Clubes, caixas, planteis e quem é humano: o mercado como está agora. Caixa ainda não aberto vale
	 * o saldo inicial (quem grava abre antes).
	 */
	private Contexto contexto(long jogoId) {
		ConfiguracaoLiga liga = jogos.carregarLiga(jogoId);
		Map<Long, List<Jogador>> planteis = jogos.carregarPlanteis(jogoId);
		Set<Long> humanos = new HashSet<>(jdbc.sql("SELECT clube_id FROM jogo_treinador WHERE jogo_id = :jogo")
			.param("jogo", jogoId)
			.query(Long.class)
			.list());
		Map<Long, Clube> clubes = new HashMap<>();
		List<ClubeNoMercado> daMaquina = new ArrayList<>();
		List<ClubeNoMercado> doUsuario = new ArrayList<>();
		for (Divisao d : liga.divisoes()) {
			for (Clube clube : d.clubes()) {
				clubes.put(clube.id(), clube);
				List<Jogador> plantel = planteis.getOrDefault(clube.id(), List.of());
				ClubeNoMercado noMercado = new ClubeNoMercado(clube, caixas.saldo(clube.id(), plantel), plantel);
				(humanos.contains(clube.id()) ? doUsuario : daMaquina).add(noMercado);
			}
		}
		for (Clube clube : liga.filaDeEspera()) {
			clubes.put(clube.id(), clube);
			if (humanos.contains(clube.id())) {
				List<Jogador> plantel = planteis.getOrDefault(clube.id(), List.of());
				doUsuario.add(new ClubeNoMercado(clube, caixas.saldo(clube.id(), plantel), plantel));
			}
		}
		Map<Long, Jogador> jogadores = new HashMap<>();
		planteis.values().forEach(p -> p.forEach(j -> jogadores.put(j.id(), j)));
		String pais = jdbc.sql("SELECT pais_codigo FROM jogo WHERE id = :jogo").param("jogo", jogoId).query(String.class).single();
		return new Contexto(pais, Moeda.doPais(pais), clubes, planteis, jogadores, daMaquina, doUsuario);
	}

	private long clubeDoUsuario(long jogoId, long usuarioId) {
		return jogos.clubeDoTreinador(jogoId, usuarioId)
			.orElseThrow(() -> new NoSuchElementException("jogo " + jogoId + " não encontrado"));
	}

	private boolean aoVivo(long jogoId) {
		return jdbc.sql("SELECT EXISTS (SELECT 1 FROM jogo_rodada_ao_vivo WHERE jogo_id = :jogo)")
			.param("jogo", jogoId)
			.query(Boolean.class)
			.single();
	}

	private boolean existemLeiloes(long temporadaId, int rodada) {
		return jdbc.sql("SELECT EXISTS (SELECT 1 FROM jogo_leilao WHERE temporada_id = :temporada AND rodada = :rodada)")
			.param("temporada", temporadaId)
			.param("rodada", rodada)
			.query(Boolean.class)
			.single();
	}

	private List<LinhaLeilao> abertos(long temporadaId, int rodada) {
		return jdbc.sql("""
				SELECT id, jogador_id, vendedor_id, lance_minimo FROM jogo_leilao
				WHERE temporada_id = :temporada AND rodada = :rodada AND situacao = 'ABERTO' ORDER BY id""")
			.param("temporada", temporadaId)
			.param("rodada", rodada)
			.query((rs, n) -> new LinhaLeilao(rs.getLong("id"), rs.getLong("jogador_id"), rs.getLong("vendedor_id"),
					rs.getLong("lance_minimo"), Situacao.ABERTO, null, null, false))
			.list();
	}

	private LinhaTemporada temporadaAtual(long jogoId) {
		return jdbc.sql("""
				SELECT id, semente, rodadas_processadas FROM jogo_temporada WHERE jogo_id = :jogo
				ORDER BY numero DESC LIMIT 1""")
			.param("jogo", jogoId)
			.query((rs, n) -> new LinhaTemporada(rs.getLong("id"), rs.getLong("semente"), rs.getInt("rodadas_processadas")))
			.optional()
			.orElseThrow(() -> new NoSuchElementException("o jogo " + jogoId + " não existe ou não tem temporada"));
	}

	public enum Situacao {
		ABERTO, VENDIDO, SEM_LANCE, CANCELADO
	}

	/**
	 * @param saldo caixa do clube do treinador
	 * @param aguardandoLances há leilões abertos esperando os lances do treinador
	 */
	public record VisaoLeiloes(int rodada, Moeda moeda, long saldo, boolean aguardandoLances, List<LeilaoNaTela> leiloes) {

		public VisaoLeiloes {
			leiloes = List.copyOf(leiloes);
		}
	}

	/**
	 * Um leilão na tela.
	 *
	 * @param estrangeiro sigla do país, se não é o da liga
	 * @param minimoNaMoeda o menor lance aceito, inteiro na moeda da liga
	 * @param impedimento por que o treinador não pode dar lance ("Caixa insuficiente", "Plantel
	 *        cheio"); nulo se pode ou se o leilão já terminou
	 * @param comprador quem levou; nulo sem venda
	 * @param seuLance o lance do treinador que valeu; nulo se ele não deu
	 * @param seu o treinador levou o jogador
	 */
	public record LeilaoNaTela(long id, Jogador jogador, String estrangeiro, Clube vendedor, long lanceMinimo,
			long minimoNaMoeda, String impedimento, Situacao situacao, Clube comprador, Long valor, boolean empate,
			Long seuLance, boolean seu) {
	}

	private record LinhaLeilao(long id, long jogadorId, long vendedorId, long lanceMinimo, Situacao situacao,
			Long compradorId, Long valor, boolean empate) {
	}

	private record LinhaTemporada(long id, long semente, int processadas) {
	}

	private record Contexto(String pais, Moeda moeda, Map<Long, Clube> clubes, Map<Long, List<Jogador>> planteis,
			Map<Long, Jogador> jogadores, List<ClubeNoMercado> daMaquina, List<ClubeNoMercado> humanos) {

		Clube clube(long id) {
			return clubes.get(id);
		}

		Jogador jogador(long id) {
			return jogadores.get(id);
		}
	}
}
