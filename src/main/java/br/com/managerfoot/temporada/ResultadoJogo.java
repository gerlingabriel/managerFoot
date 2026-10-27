package br.com.managerfoot.temporada;

import java.util.Objects;

/** Placar final de um jogo do calendário. */
public record ResultadoJogo(Jogo jogo, int golsMandante, int golsVisitante) {

	public ResultadoJogo {
		Objects.requireNonNull(jogo, "jogo é obrigatório");
		if (golsMandante < 0 || golsVisitante < 0) {
			throw new IllegalArgumentException("gols não podem ser negativos: " + golsMandante + "x" + golsVisitante);
		}
	}
}
