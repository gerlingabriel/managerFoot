package br.com.managerfoot.persistencia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.economia.CaixaDaRodada;
import br.com.managerfoot.economia.MercadoDaMaquina;
import br.com.managerfoot.economia.Moeda;
import br.com.managerfoot.economia.PedidoDeAumento;
import br.com.managerfoot.economia.PedidoDeAumento.Candidato;
import br.com.managerfoot.economia.PrecoJogador;

/**
 * Salário gravado e pedido de aumento dos jogadores do usuário (SPEC §9.5, rev. 58, modelo 10a). O
 * salário de um jogador de clube de usuário fica na força do último contrato; os da máquina seguem a
 * força atual (readequam sempre). Quando um jogador sem contrato renovado na temporada sobe o
 * intervalo acima da força do salário, ele pede aumento: um por rodada no clube, e a rodada só começa
 * depois da resposta. Aceitar renova pela força atual; recusar põe o jogador em leilão na hora, com o
 * mínimo no lance inicial de leilão (preço −20%).
 */
@Repository
public class AumentoRepositorio {

	private final JdbcClient jdbc;
	private final JogoRepositorio jogos;
	private final CaixaRepositorio caixas;
	private final LeilaoRepositorio leiloes;
	private final PedidoDeAumento pedido = PedidoDeAumento.padrao();
	private final CaixaDaRodada caixa = CaixaDaRodada.padrao();
	private final PrecoJogador preco = PrecoJogador.padrao();
	private final MercadoDaMaquina mercado = MercadoDaMaquina.padrao();

	public AumentoRepositorio(JdbcClient jdbc, JogoRepositorio jogos, CaixaRepositorio caixas, LeilaoRepositorio leiloes) {
		this.jdbc = jdbc;
		this.jogos = jogos;
		this.caixas = caixas;
		this.leiloes = leiloes;
	}

	/**
	 * Grava o salário pela força atual dos jogadores que entraram num clube de usuário sem salário
	 * gravado (início do jogo, clube novo do treinador demitido) e solta o dos clubes que deixaram de ser
	 * de usuário, que passam a readequar como a máquina.
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void fixarSalarios(long jogoId) {
		jdbc.sql("""
				UPDATE jogo_jogador j SET forca_salario = j.forca
				FROM jogo_treinador t WHERE t.jogo_id = :jogo AND t.clube_id = j.clube_id AND j.forca_salario IS NULL""")
			.param("jogo", jogoId)
			.update();
		jdbc.sql("""
				UPDATE jogo_jogador j SET forca_salario = NULL
				FROM jogo_clube c WHERE c.id = j.clube_id AND c.jogo_id = :jogo AND j.forca_salario IS NOT NULL
				  AND NOT EXISTS (SELECT 1 FROM jogo_treinador t WHERE t.clube_id = c.id)""")
			.param("jogo", jogoId)
			.update();
	}

	/**
	 * O pedido de aumento do clube do usuário nesta rodada, se houver.
	 *
	 * @throws NoSuchElementException se o usuário não treina clube no jogo
	 */
	@Transactional(readOnly = true)
	public Optional<VisaoPedido> pendente(long jogoId, long usuarioId) {
		long clube = clubeDoUsuario(jogoId, usuarioId);
		Situacao s = situacao(jogoId);
		return quemPede(jogoId, clube, s).map(c -> {
			Jogador j = c.jogador();
			Moeda moeda = Moeda.doPais(s.pais());
			long folha = caixas.folhas(jogoId).getOrDefault(clube, 0L);
			long atual = caixa.salario(c.forcaDoSalario());
			long pedido = caixa.salario(j.forca());
			return new VisaoPedido(jogos.ano(jogoId, s.numero()), s.processadas() + 1, j, atual, pedido, folha,
					folha - atual + pedido, preco.lanceInicialLeilao(j, s.pais()), moeda);
		});
	}

	/** Se algum clube de usuário tem pedido de aumento sem resposta: a rodada não começa. */
	@Transactional(readOnly = true)
	public boolean algumPendente(long jogoId) {
		Situacao s = situacao(jogoId);
		return jdbc.sql("SELECT clube_id FROM jogo_treinador WHERE jogo_id = :jogo").param("jogo", jogoId)
			.query(Long.class)
			.list()
			.stream()
			.anyMatch(clube -> quemPede(jogoId, clube, s).isPresent());
	}

	/**
	 * Aceita o pedido: o salário passa à força atual, o contrato vale até o fim da temporada e o jogador
	 * não pode ser vendido nesta rodada.
	 *
	 * @return se havia esse pedido; um segundo pedido não faz nada
	 */
	@Transactional
	public boolean aceitar(long jogoId, long usuarioId, long jogadorId) {
		jogos.travar(jogoId);
		long clube = clubeDoUsuario(jogoId, usuarioId);
		Situacao s = situacao(jogoId);
		if (!pedeAgora(jogoId, clube, s, jogadorId)) {
			return false;
		}
		jdbc.sql("""
				UPDATE jogo_jogador SET forca_salario = forca, contrato_temporada = :numero, aumento_temporada = :numero,
				       aumento_rodada = :rodada
				WHERE id = :jogador""")
			.param("numero", s.numero())
			.param("rodada", s.processadas())
			.param("jogador", jogadorId)
			.update();
		return true;
	}

	/**
	 * Recusa o pedido: o jogador vai a leilão na hora, com os lances da máquina. Sem lance, ou se ele não
	 * pode sair (plantel no mínimo da posição), fica no clube com o salário de antes e não pede de novo
	 * na temporada.
	 *
	 * @return o leilão, para mostrar o resultado; vazio se não houve leilão ou se não havia esse pedido
	 */
	@Transactional
	public Optional<Long> recusar(long jogoId, long usuarioId, long jogadorId) {
		jogos.travar(jogoId);
		long clube = clubeDoUsuario(jogoId, usuarioId);
		Situacao s = situacao(jogoId);
		if (!pedeAgora(jogoId, clube, s, jogadorId)) {
			return Optional.empty();
		}
		jdbc.sql("UPDATE jogo_jogador SET aumento_temporada = :numero, aumento_rodada = :rodada WHERE id = :jogador")
			.param("numero", s.numero())
			.param("rodada", s.processadas())
			.param("jogador", jogadorId)
			.update();
		List<Jogador> plantel = jogos.carregarPlanteis(jogoId).getOrDefault(clube, List.of());
		Jogador jogador = plantel.stream().filter(j -> j.id() == jogadorId).findFirst().orElseThrow();
		// as recusas do leilão pela ficha desfariam a resposta: confere antes
		boolean jaFoi = jdbc.sql("""
				SELECT EXISTS (SELECT 1 FROM jogo_leilao WHERE temporada_id = :temporada AND rodada = :rodada
				                                            AND jogador_id = :jogador)""")
			.param("temporada", s.temporadaId())
			.param("rodada", s.processadas() + 1)
			.param("jogador", jogadorId)
			.query(Boolean.class)
			.single();
		if (jaFoi || !mercado.podeSair(plantel, jogador)) {
			return Optional.empty();
		}
		long minimo = Moeda.doPais(s.pais()).unidadesAcima(preco.lanceInicialLeilao(jogador, s.pais()));
		return Optional.of(leiloes.venderPorLeilao(jogoId, usuarioId, jogadorId, minimo));
	}

	private boolean pedeAgora(long jogoId, long clube, Situacao s, long jogadorId) {
		return quemPede(jogoId, clube, s).filter(c -> c.jogador().id() == jogadorId).isPresent();
	}

	/**
	 * Quem pede aumento no clube agora: nenhum durante a rodada ao vivo, com a temporada já jogada ou se
	 * o clube já respondeu um pedido nesta rodada; senão, o que mais subiu entre os sem contrato
	 * renovado que ainda não responderam nesta temporada.
	 */
	private Optional<Candidato> quemPede(long jogoId, long clube, Situacao s) {
		if (s.aoVivo() || s.processadas() >= s.total()) {
			return Optional.empty();
		}
		boolean respondeu = jdbc.sql("""
				SELECT EXISTS (SELECT 1 FROM jogo_jogador WHERE clube_id = :clube AND aumento_temporada = :numero
				                                            AND aumento_rodada = :rodada)""")
			.param("clube", clube)
			.param("numero", s.numero())
			.param("rodada", s.processadas())
			.query(Boolean.class)
			.single();
		if (respondeu) {
			return Optional.empty();
		}
		Map<Long, Integer> forcaDoSalario = new HashMap<>();
		jdbc.sql("""
				SELECT id, forca_salario FROM jogo_jogador
				WHERE clube_id = :clube AND forca_salario IS NOT NULL
				  AND contrato_temporada IS DISTINCT FROM :numero AND aumento_temporada IS DISTINCT FROM :numero""")
			.param("clube", clube)
			.param("numero", s.numero())
			.query(rs -> {
				forcaDoSalario.put(rs.getLong("id"), rs.getInt("forca_salario"));
			});
		List<Candidato> candidatos = new ArrayList<>();
		for (Jogador j : jogos.carregarPlanteis(jogoId).getOrDefault(clube, List.of())) {
			Integer forca = forcaDoSalario.get(j.id());
			if (forca != null) {
				candidatos.add(new Candidato(j, forca));
			}
		}
		return pedido.quemPede(candidatos);
	}

	private Situacao situacao(long jogoId) {
		return jdbc.sql("""
				SELECT t.id, t.numero, t.rodadas_processadas, j.pais_codigo,
				       (SELECT coalesce(max(p.rodada), 0) FROM jogo_partida p WHERE p.temporada_id = t.id) AS total,
				       EXISTS (SELECT 1 FROM jogo_rodada_ao_vivo r WHERE r.jogo_id = j.id) AS ao_vivo
				FROM jogo_temporada t JOIN jogo j ON j.id = t.jogo_id
				WHERE t.jogo_id = :jogo ORDER BY t.numero DESC LIMIT 1""")
			.param("jogo", jogoId)
			.query((rs, n) -> new Situacao(rs.getLong("id"), rs.getInt("numero"), rs.getInt("rodadas_processadas"),
					rs.getInt("total"), rs.getString("pais_codigo"), rs.getBoolean("ao_vivo")))
			.optional()
			.orElseThrow(() -> new NoSuchElementException("o jogo " + jogoId + " não tem temporada"));
	}

	private long clubeDoUsuario(long jogoId, long usuarioId) {
		return jogos.clubeDoTreinador(jogoId, usuarioId)
			.orElseThrow(() -> new NoSuchElementException("jogo " + jogoId + " não encontrado"));
	}

	private record Situacao(long temporadaId, int numero, int processadas, int total, String pais, boolean aoVivo) {
	}

	/**
	 * O pedido de aumento na caixa do plantel (modelo 10a), em centavos de Real.
	 *
	 * @param temporada ano da temporada
	 * @param rodada a próxima rodada, que espera a resposta
	 * @param salarioAtual salário por rodada do contrato
	 * @param salarioPedido salário da força atual
	 * @param lanceMinimo mínimo do leilão se recusar
	 */
	public record VisaoPedido(int temporada, int rodada, Jogador jogador, long salarioAtual, long salarioPedido,
			long folhaAtual, long folhaNova, long lanceMinimo, Moeda moeda) {

		public long diferenca() {
			return salarioPedido - salarioAtual;
		}

		/** Aumento em %, arredondado. */
		public long percentual() {
			return salarioAtual == 0 ? 0 : Math.round(diferenca() * 100.0 / salarioAtual);
		}
	}
}
