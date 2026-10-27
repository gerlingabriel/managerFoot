package br.com.managerfoot.motor;

import br.com.managerfoot.dominio.Posicao;

/**
 * Probabilidade (em %) de o finalizador sair de cada setor (SPEC §7.2).
 * A soma deve ser 100.
 */
public record PesosFinalizador(double atacante, double meioCampista, double defensor, double goleiro) {

	private static final double TOLERANCIA = 1e-6;

	public PesosFinalizador {
		double soma = atacante + meioCampista + defensor + goleiro;
		if (atacante < 0 || meioCampista < 0 || defensor < 0 || goleiro < 0 || Math.abs(soma - 100) > TOLERANCIA) {
			throw new IllegalArgumentException(
					"pesos do finalizador devem ser não negativos e somar 100, mas somam " + soma);
		}
	}

	public double peso(Posicao posicao) {
		return switch (posicao) {
			case GOLEIRO -> goleiro;
			case DEFENSOR -> defensor;
			case MEIO_CAMPISTA -> meioCampista;
			case ATACANTE -> atacante;
		};
	}
}
