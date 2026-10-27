package br.com.managerfoot.motor;

/**
 * Efeito da sorte e do azar da temporada (SPEC §5.3.1). Meta: cerca de 5 pontos percentuais a mais
 * de vitórias para o beneficiado e de derrotas para o prejudicado, contra adversário de mesma força.
 *
 * @param chancesExtras finalizações extras do beneficiado, como fração de {@code chancePorMinuto}
 * @param fatorDominioPrejudicado multiplicador do domínio do meio-campo do prejudicado (menor que 1)
 */
public record ParametrosSorte(double chancesExtras, double fatorDominioPrejudicado) {

	public ParametrosSorte {
		if (chancesExtras < 0 || chancesExtras > 1) {
			throw new IllegalArgumentException("chancesExtras deve estar entre 0 e 1, mas foi " + chancesExtras);
		}
		if (fatorDominioPrejudicado <= 0 || fatorDominioPrejudicado > 1) {
			throw new IllegalArgumentException(
					"fatorDominioPrejudicado deve estar entre 0 (exclusivo) e 1, mas foi " + fatorDominioPrejudicado);
		}
	}

	public static ParametrosSorte padrao() {
		// calibrado por simulação: docs/simulacoes/calibracao-motor.md
		return new ParametrosSorte(0.095, 0.83);
	}
}
