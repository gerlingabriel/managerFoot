package br.com.managerfoot.economia;

import br.com.managerfoot.dominio.Jogador;

/**
 * Preço do jogador pela força (SPEC §9.4, rev. 40 e 41): a compra direta paga o preço; o leilão começa
 * com o desconto. Em centavos de Real.
 */
public final class PrecoJogador {

	private final ParametrosMercado parametros;
	private final CaixaDaRodada caixa;

	public PrecoJogador(ParametrosMercado parametros, CaixaDaRodada caixa) {
		this.parametros = parametros;
		this.caixa = caixa;
	}

	public static PrecoJogador padrao() {
		return new PrecoJogador(ParametrosMercado.padrao(), CaixaDaRodada.padrao());
	}

	/** Preço da compra direta. */
	public long preco(Jogador jogador) {
		return redondo(caixa.salario(jogador) * parametros.rodadasDeSalarioNoPreco());
	}

	/** Lance inicial do leilão: o preço com o desconto. */
	public long lanceInicialLeilao(Jogador jogador) {
		return redondo(Math.round(preco(jogador) * (1 - parametros.descontoLeilao())));
	}

	private long redondo(long centavos) {
		return Math.max(parametros.arredondamento(),
				Math.round((double) centavos / parametros.arredondamento()) * parametros.arredondamento());
	}
}
