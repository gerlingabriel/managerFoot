package br.com.managerfoot.motor;

import java.util.Objects;
import java.util.SplittableRandom;

import br.com.managerfoot.dominio.Escalacao;

/** Ponto de entrada do motor: cria partidas reproduzíveis a partir de uma semente. */
public final class MotorPartida {

	private final ParametrosMotor parametros;

	public MotorPartida(ParametrosMotor parametros) {
		this.parametros = Objects.requireNonNull(parametros);
	}

	/** Partida para avançar minuto a minuto (exibição ao vivo). */
	public Partida iniciar(Escalacao casa, Escalacao fora, long semente) {
		return new Partida(parametros, casa, fora, new SplittableRandom(semente));
	}

	/** Simula os 90 minutos de uma vez (processamento rápido). */
	public ResultadoPartida simular(Escalacao casa, Escalacao fora, long semente) {
		Partida partida = iniciar(casa, fora, semente);
		while (!partida.terminada()) {
			partida.avancarMinuto();
		}
		return partida.resultado();
	}
}
