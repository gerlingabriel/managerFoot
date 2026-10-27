package br.com.managerfoot.motor;

import br.com.managerfoot.dominio.Jogador;

/**
 * Algo que aconteceu na partida, atribuído a um time e a um jogador.
 *
 * @param penalti a finalização foi uma cobrança de pênalti
 */
public record EventoPartida(int minuto, Lado lado, TipoEvento tipo, Jogador jogador, boolean penalti) {

	public EventoPartida(int minuto, Lado lado, TipoEvento tipo, Jogador jogador) {
		this(minuto, lado, tipo, jogador, false);
	}
}
