package br.com.managerfoot.temporada;

import br.com.managerfoot.dominio.Clube;

/** Uma partida do calendário. */
public record Jogo(int rodada, Clube mandante, Clube visitante) {

	public boolean envolve(Clube clube) {
		return mandante.equals(clube) || visitante.equals(clube);
	}
}
