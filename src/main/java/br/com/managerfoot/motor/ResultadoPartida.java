package br.com.managerfoot.motor;

import java.util.List;

/** Placar final e todos os eventos, em ordem cronológica. */
public record ResultadoPartida(int golsCasa, int golsFora, List<EventoPartida> eventos) {

	public ResultadoPartida {
		eventos = List.copyOf(eventos);
	}
}
