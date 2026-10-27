package br.com.managerfoot.motor;

/** Lado do time na partida. */
public enum Lado {
	CASA,
	FORA;

	public Lado adversario() {
		return this == CASA ? FORA : CASA;
	}
}
