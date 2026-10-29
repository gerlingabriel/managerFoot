package br.com.managerfoot.motor;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Placar final e todos os eventos, em ordem cronológica. O placar inclui a prorrogação; a disputa
 * de pênaltis fica à parte.
 *
 * @param prorrogacao a partida teve prorrogação (SPEC §8.2)
 * @param penaltis a disputa de pênaltis, se houve
 */
public record ResultadoPartida(int golsCasa, int golsFora, List<EventoPartida> eventos, boolean prorrogacao,
		Optional<DisputaPenaltis> penaltis) {

	public ResultadoPartida {
		eventos = List.copyOf(eventos);
		Objects.requireNonNull(penaltis);
		if (penaltis.isPresent() && !prorrogacao) {
			throw new IllegalArgumentException("disputa de pênaltis sem prorrogação");
		}
	}

	public ResultadoPartida(int golsCasa, int golsFora, List<EventoPartida> eventos) {
		this(golsCasa, golsFora, eventos, false, Optional.empty());
	}
}
