package br.com.managerfoot.temporada;

import java.util.List;
import java.util.Optional;

import br.com.managerfoot.dominio.Clube;

/**
 * Jogos de uma rodada.
 *
 * @param folga clube que não joga na rodada (só com número ímpar de clubes)
 */
public record Rodada(int numero, List<Jogo> jogos, Optional<Clube> folga) {

	public Rodada {
		jogos = List.copyOf(jogos);
	}
}
