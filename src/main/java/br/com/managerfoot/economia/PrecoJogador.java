package br.com.managerfoot.economia;

import br.com.managerfoot.dominio.Jogador;

/**
 * Preço do jogador pela força (SPEC §9.4, rev. 40 a 43): a compra direta paga o preço; o leilão começa
 * com o desconto. Na rev. 65 (em simulação) a faixa pode seguir o clube que compra. Em centavos de Real.
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

	/**
	 * Preço da compra direta; o estrangeiro (de país diferente do da liga) sai com desconto e o
	 * asterisco, com acréscimo.
	 *
	 * @param paisDaLiga código do país da liga em que o jogador está
	 */
	public long preco(Jogador jogador, String paisDaLiga) {
		return redondo(Math.round(semArredondar(jogador, paisDaLiga)));
	}

	/**
	 * Preço para um comprador de força em campo dada: com o preço pelo comprador (rev. 65), o preço sem
	 * faixa com a faixa do clube que compra; sem ele, o mesmo de {@link #preco(Jogador, String)}.
	 */
	public long preco(Jogador jogador, String paisDaLiga, double forcaComprador) {
		if (!parametros.precoPeloComprador()) {
			return preco(jogador, paisDaLiga);
		}
		return redondo(Math.round(semArredondar(jogador, paisDaLiga) * caixa.fatorDaFaixa(forcaComprador)));
	}

	private double semArredondar(Jogador jogador, String paisDaLiga) {
		double salario = parametros.precoPeloComprador() ? caixa.salarioSemFaixa(jogador.forca()) : caixa.salario(jogador);
		double preco = salario * parametros.rodadasDeSalarioNoPreco();
		if (!jogador.nacionalidade().equals(paisDaLiga)) {
			preco *= 1 - parametros.descontoEstrangeiro();
		}
		if (jogador.asterisco()) {
			preco *= 1 + parametros.acrescimoAsterisco();
		}
		return preco;
	}

	/** Lance inicial do leilão: o preço com o desconto do leilão. */
	public long lanceInicialLeilao(Jogador jogador, String paisDaLiga) {
		return redondo(Math.round(preco(jogador, paisDaLiga) * (1 - parametros.descontoLeilao())));
	}

	/** Arredonda para o múltiplo do mercado (mínimo de um múltiplo). */
	long redondo(long centavos) {
		return Math.max(parametros.arredondamento(),
				Math.round((double) centavos / parametros.arredondamento()) * parametros.arredondamento());
	}
}
