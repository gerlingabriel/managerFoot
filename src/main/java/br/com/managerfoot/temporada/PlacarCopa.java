package br.com.managerfoot.temporada;

import java.util.Objects;
import java.util.Optional;

/**
 * Placar de uma partida da copa, com a prorrogação no placar e a disputa de pênaltis à parte.
 *
 * @param prorrogacao a partida teve prorrogação
 * @param penaltis a disputa, se houve
 */
public record PlacarCopa(int golsMandante, int golsVisitante, boolean prorrogacao, Optional<Penaltis> penaltis) {

	/** Gols de cada lado na disputa de pênaltis. */
	public record Penaltis(int mandante, int visitante) {

		public Penaltis {
			if (mandante < 0 || visitante < 0 || mandante == visitante) {
				throw new IllegalArgumentException("disputa de pênaltis inválida: " + mandante + "x" + visitante);
			}
		}
	}

	public PlacarCopa {
		if (golsMandante < 0 || golsVisitante < 0) {
			throw new IllegalArgumentException("gols não podem ser negativos: " + golsMandante + "x" + golsVisitante);
		}
		Objects.requireNonNull(penaltis);
		if (penaltis.isPresent() && !prorrogacao) {
			throw new IllegalArgumentException("disputa de pênaltis sem prorrogação");
		}
	}

	/** Placar dos 90 minutos, sem prorrogação. */
	public static PlacarCopa de(int golsMandante, int golsVisitante) {
		return new PlacarCopa(golsMandante, golsVisitante, false, Optional.empty());
	}
}
