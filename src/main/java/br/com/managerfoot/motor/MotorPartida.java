package br.com.managerfoot.motor;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SplittableRandom;

import br.com.managerfoot.dominio.Escalacao;
import br.com.managerfoot.dominio.Sorte;

/** Ponto de entrada do motor: cria partidas reproduzíveis a partir de uma semente. */
public final class MotorPartida {

	private final ParametrosMotor parametros;

	public MotorPartida(ParametrosMotor parametros) {
		this.parametros = Objects.requireNonNull(parametros);
	}

	/** Partida para avançar minuto a minuto (exibição ao vivo), com times de usuários e sorte da temporada. */
	public Partida iniciar(RegrasPartida regras, Escalacao casa, Escalacao fora, Set<Lado> ladosDoUsuario,
			Map<Lado, Sorte> sorte, long semente) {
		return new Partida(parametros, regras, casa, fora, ladosDoUsuario, sorte, new SplittableRandom(semente));
	}

	/** Partida para avançar minuto a minuto (exibição ao vivo), com times de usuários. */
	public Partida iniciar(RegrasPartida regras, Escalacao casa, Escalacao fora, Set<Lado> ladosDoUsuario,
			long semente) {
		return iniciar(regras, casa, fora, ladosDoUsuario, Map.of(), semente);
	}

	/** Partida minuto a minuto entre times da máquina, com regras padrão. */
	public Partida iniciar(Escalacao casa, Escalacao fora, long semente) {
		return iniciar(RegrasPartida.padrao(), casa, fora, Set.of(), semente);
	}

	/** Simula os 90 minutos de uma vez (processamento rápido), com a sorte da temporada. */
	public ResultadoPartida simular(RegrasPartida regras, Escalacao casa, Escalacao fora, Map<Lado, Sorte> sorte,
			long semente) {
		Partida partida = iniciar(regras, casa, fora, Set.of(), sorte, semente);
		while (!partida.terminada()) {
			partida.avancarMinuto();
		}
		return partida.resultado();
	}

	/** Simula os 90 minutos de uma vez (processamento rápido, sem usuários). */
	public ResultadoPartida simular(RegrasPartida regras, Escalacao casa, Escalacao fora, long semente) {
		Partida partida = iniciar(regras, casa, fora, Set.of(), semente);
		while (!partida.terminada()) {
			partida.avancarMinuto();
		}
		return partida.resultado();
	}

	/** Simula os 90 minutos de uma vez com as regras padrão. */
	public ResultadoPartida simular(Escalacao casa, Escalacao fora, long semente) {
		Partida partida = iniciar(casa, fora, semente);
		while (!partida.terminada()) {
			partida.avancarMinuto();
		}
		return partida.resultado();
	}
}
