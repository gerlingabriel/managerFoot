package br.com.managerfoot.motor;

/**
 * Parâmetros de pênalti e goleiro improvisado (SPEC §7.3, §12). Valores provisórios.
 *
 * @param penaltisPorMinuto chance de pênalti por minuto (meta da §7.6: ~0,3 por jogo)
 * @param conversao probabilidade de a cobrança virar gol
 * @param fatorGoleiroImprovisado fração da força de um jogador de linha atuando no gol
 */
public record ParametrosPenalti(double penaltisPorMinuto, double conversao, double fatorGoleiroImprovisado) {

	public ParametrosPenalti {
		if (penaltisPorMinuto < 0 || penaltisPorMinuto > 1 || conversao < 0 || conversao > 1
				|| fatorGoleiroImprovisado <= 0 || fatorGoleiroImprovisado > 1) {
			throw new IllegalArgumentException(
					"penaltisPorMinuto e conversao devem estar entre 0 e 1; fatorGoleiroImprovisado entre 0 (exclusivo) e 1");
		}
	}

	public static ParametrosPenalti padrao() {
		return new ParametrosPenalti(0.3 / 90, 0.75, 0.5);
	}
}
