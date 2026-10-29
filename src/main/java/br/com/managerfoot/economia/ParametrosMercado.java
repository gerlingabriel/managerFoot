package br.com.managerfoot.economia;

/**
 * Valores do mercado (SPEC §9.4, rev. 41 e 43). O preço segue a curva do salário: o agente propôs um
 * número de rodadas de salário, validado por simulação ({@code docs/simulacoes/economia-caixa.md}).
 *
 * @param rodadasDeSalarioNoPreco preço do jogador, em rodadas do salário dele (proposta)
 * @param descontoLeilao desconto do lance inicial do leilão sobre o preço (definido pelo usuário: 20%)
 * @param descontoEstrangeiro desconto do jogador de outro país sobre o preço (definido pelo usuário: 25%)
 * @param acrescimoAsterisco acréscimo do jogador com asterisco sobre o preço (definido pelo usuário: 25%)
 * @param arredondamento o preço é arredondado para múltiplos deste valor, em centavos de Real
 * @param precoSemFaixa o preço vem do salário sem a faixa da rev. 61: só a força do jogador (e o
 *        asterisco) pesa (rev. 66, pelo usuário)
 */
public record ParametrosMercado(int rodadasDeSalarioNoPreco, double descontoLeilao, double descontoEstrangeiro,
		double acrescimoAsterisco, long arredondamento, boolean precoSemFaixa) {

	public ParametrosMercado {
		if (rodadasDeSalarioNoPreco < 1 || descontoLeilao < 0 || descontoLeilao >= 1 || descontoEstrangeiro < 0
				|| descontoEstrangeiro >= 1 || acrescimoAsterisco < 0 || arredondamento < 1) {
			throw new IllegalArgumentException("parâmetros de mercado inválidos");
		}
	}

	/**
	 * Proposta da rev. 41 (80 rodadas de salário, valores redondos em R$ 1.000) e regras do usuário
	 * (leilão 20% abaixo; estrangeiro 25% mais barato e asterisco 25% mais caro, rev. 43; preço sem a
	 * faixa do salário, rev. 66).
	 */
	public static ParametrosMercado padrao() {
		return new ParametrosMercado(80, 0.20, 0.25, 0.25, 1_000_00L, true);
	}

	/** Os mesmos valores com o preço sem faixa ligado ou desligado (rev. 66). */
	public ParametrosMercado comPrecoSemFaixa(boolean semFaixa) {
		return new ParametrosMercado(rodadasDeSalarioNoPreco, descontoLeilao, descontoEstrangeiro, acrescimoAsterisco,
				arredondamento, semFaixa);
	}
}
