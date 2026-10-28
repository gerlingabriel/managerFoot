package br.com.managerfoot.economia;

/**
 * Valores do mercado (SPEC §9.4, rev. 41). O preço segue a curva do salário: o agente propôs um
 * número de rodadas de salário, validado por simulação ({@code docs/simulacoes/economia-caixa.md}).
 *
 * @param rodadasDeSalarioNoPreco preço do jogador, em rodadas do salário dele (proposta)
 * @param descontoLeilao desconto do lance inicial do leilão sobre o preço (definido pelo usuário: 20%)
 * @param arredondamento o preço é arredondado para múltiplos deste valor, em centavos de Real
 */
public record ParametrosMercado(int rodadasDeSalarioNoPreco, double descontoLeilao, long arredondamento) {

	public ParametrosMercado {
		if (rodadasDeSalarioNoPreco < 1 || descontoLeilao < 0 || descontoLeilao >= 1 || arredondamento < 1) {
			throw new IllegalArgumentException("parâmetros de mercado inválidos");
		}
	}

	/** Proposta da rev. 41: 80 rodadas de salário, leilão 20% abaixo, valores redondos em R$ 1.000. */
	public static ParametrosMercado padrao() {
		return new ParametrosMercado(80, 0.20, 1_000_00L);
	}
}
