package br.com.managerfoot.persistencia;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.managerfoot.economia.TipoLancamento;

/** Jogador que muda de clube por compra direta ou leilão (SPEC §9.4). */
@Repository
public class TransferenciaRepositorio {

	private final JdbcClient jdbc;
	private final CaixaRepositorio caixas;

	public TransferenciaRepositorio(JdbcClient jdbc, CaixaRepositorio caixas) {
		this.jdbc = jdbc;
		this.caixas = caixas;
	}

	/**
	 * Passa o jogador ao comprador: sai da escalação do vendedor e do anúncio, entra fora do banco do
	 * comprador, e os dois ganham os lançamentos de compra e venda. Quem chama confere o negócio e
	 * abre a transação.
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void transferir(long jogoId, long jogadorId, long vendedor, long comprador, long valor) {
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
	}
}
