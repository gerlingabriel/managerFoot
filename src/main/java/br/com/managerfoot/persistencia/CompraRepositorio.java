package br.com.managerfoot.persistencia;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
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

	private String paisDaLiga(long jogoId) {
		return jdbc.sql("SELECT pais_codigo FROM jogo WHERE id = :jogo")
			.param("jogo", jogoId)
			.query(String.class)
			.single();
	}
}
