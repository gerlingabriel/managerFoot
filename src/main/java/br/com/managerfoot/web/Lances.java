package br.com.managerfoot.web;

import org.springframework.stereotype.Component;

import br.com.managerfoot.motor.EventoPartida;
import br.com.managerfoot.motor.Lado;

/**
 * Texto e ícone de um lance na tela da rodada (SPEC §10.5, rev. 33): "Nome 41'" e "(pên.)" no gol
 * de pênalti. O pênalti defendido fica com o goleiro: "Goleiro defende pênalti 41'". Usado como
 * {@code @lances.texto(lance)}.
 */
@Component("lances")
public class Lances {

	public String texto(EventoPartida lance) {
		if (lance.penaltiDefendido()) {
			return lance.goleiro().nome() + " defende pênalti " + lance.minuto() + "'";
		}
		return lance.jogador().nome() + " " + lance.minuto() + "'" + (lance.penalti() ? " (pên.)" : "");
	}

	/** Na janela do jogo: o nome no lado do lance; o pênalti defendido fica do lado do goleiro. */
	public String nome(EventoPartida lance) {
		if (lance.penaltiDefendido()) {
			return lance.goleiro().nome() + " defende pênalti de " + lance.jogador().nome();
		}
		return lance.jogador().nome() + (lance.penalti() ? " (pên.)" : "");
	}

	/** O time do lance na tela: o do goleiro, no pênalti defendido. */
	public boolean daCasa(EventoPartida lance) {
		return (lance.penaltiDefendido() ? lance.lado().adversario() : lance.lado()) == Lado.CASA;
	}

	/** Nome do lance para leitores de tela. */
	public String descricao(EventoPartida lance) {
		if (lance.penaltiDefendido()) {
			return "Pênalti defendido por " + lance.goleiro().nome();
		}
		String tipo = switch (lance.tipo()) {
			case GOL -> "Gol";
			case CARTAO_AMARELO -> "Cartão amarelo";
			case LESAO -> "Lesão";
			default -> "Expulsão";
		};
		return tipo + " de " + lance.jogador().nome();
	}

	/** Classe do ícone: gol, amarelo, lesão, defesa de pênalti ou vermelho (o segundo amarelo também é expulsão). */
	public String icone(EventoPartida lance) {
		return switch (lance.tipo()) {
			case GOL -> "gol";
			case DEFESA -> "defesa";
			case CARTAO_AMARELO -> "amarelo";
			case LESAO -> "lesao";
			default -> "vermelho";
		};
	}

	/** Substituição feita: "46' Viana ↔ Uchôa" (quem saiu ↔ quem entrou). */
	public String substituicao(EventoPartida troca) {
		return troca.minuto() + "' " + troca.substituido().nome() + " ↔ " + troca.jogador().nome();
	}
}
