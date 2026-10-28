package br.com.managerfoot.web;

import org.springframework.stereotype.Component;

import br.com.managerfoot.motor.EventoPartida;

/**
 * Texto e ícone de um lance na tela da rodada (SPEC §10.5, rev. 33): "Nome 41'" e "(pên.)" no gol
 * de pênalti. Usado como {@code @lances.texto(lance)}.
 */
@Component("lances")
public class Lances {

	public String texto(EventoPartida lance) {
		return lance.jogador().nome() + " " + lance.minuto() + "'" + (lance.penalti() ? " (pên.)" : "");
	}

	/** Classe do ícone: gol, amarelo ou vermelho (o segundo amarelo também é expulsão). */
	public String icone(EventoPartida lance) {
		return switch (lance.tipo()) {
			case GOL -> "gol";
			case CARTAO_AMARELO -> "amarelo";
			default -> "vermelho";
		};
	}
}
