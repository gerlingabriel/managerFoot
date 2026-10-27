package br.com.managerfoot.motor;

import br.com.managerfoot.dominio.Jogador;

/**
 * Algo que aconteceu na partida, atribuído a um time e a um jogador.
 *
 * @param penalti a finalização foi uma cobrança de pênalti
 * @param substituido em uma {@link TipoEvento#SUBSTITUICAO}, quem saiu; nos demais eventos, nulo
 */
public record EventoPartida(int minuto, Lado lado, TipoEvento tipo, Jogador jogador, boolean penalti,
		Jogador substituido) {

	public EventoPartida(int minuto, Lado lado, TipoEvento tipo, Jogador jogador) {
		this(minuto, lado, tipo, jogador, false, null);
	}

	public EventoPartida(int minuto, Lado lado, TipoEvento tipo, Jogador jogador, boolean penalti) {
		this(minuto, lado, tipo, jogador, penalti, null);
	}

	static EventoPartida substituicao(int minuto, Lado lado, Jogador entrou, Jogador saiu) {
		return new EventoPartida(minuto, lado, TipoEvento.SUBSTITUICAO, entrou, false, saiu);
	}
}
