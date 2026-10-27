package br.com.managerfoot.motor;

import br.com.managerfoot.dominio.Jogador;

/** Algo que aconteceu na partida, atribuído a um time e a um jogador. */
public record EventoPartida(int minuto, Lado lado, TipoEvento tipo, Jogador jogador) {
}
