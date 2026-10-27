package br.com.managerfoot.motor;

/**
 * Efeitos do asterisco (SPEC §5.3). Valores provisórios.
 *
 * @param bonusConversao aumento relativo da chance de gol do jogador de linha com asterisco, em
 *        todas as finalizações, inclusive pênaltis; o bônus de artilharia soma outro igual
 * @param fatorEscolhaFinalizador multiplicador da força do jogador com asterisco no sorteio do finalizador
 * @param conversaoPenaltiGoleiroAsterisco conversão do pênalti contra goleiro com asterisco, no lugar
 *        da conversão normal
 * @param limiteSemPenalidade asteriscos em campo sem penalidade
 * @param penalidadeDominioPorExcedente fração do domínio do meio-campo perdida por asterisco além do limite
 */
public record ParametrosAsterisco(double bonusConversao, double fatorEscolhaFinalizador,
		double conversaoPenaltiGoleiroAsterisco, int limiteSemPenalidade, double penalidadeDominioPorExcedente) {

	public ParametrosAsterisco {
		if (bonusConversao < 0 || fatorEscolhaFinalizador < 1 || conversaoPenaltiGoleiroAsterisco < 0
				|| conversaoPenaltiGoleiroAsterisco > 1 || limiteSemPenalidade < 0
				|| penalidadeDominioPorExcedente < 0 || penalidadeDominioPorExcedente >= 1) {
			throw new IllegalArgumentException("parâmetros do asterisco inválidos: bonusConversao >= 0, "
					+ "fatorEscolhaFinalizador >= 1, conversaoPenaltiGoleiroAsterisco entre 0 e 1, "
					+ "limiteSemPenalidade >= 0, penalidadeDominioPorExcedente entre 0 e 1 (exclusivo)");
		}
	}

	public static ParametrosAsterisco padrao() {
		// penalidade calibrada por simulação: docs/simulacoes/calibracao-motor.md
		return new ParametrosAsterisco(0.15, 1.5, 0.65, 3, 0.01);
	}
}
