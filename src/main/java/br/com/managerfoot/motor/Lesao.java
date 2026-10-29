package br.com.managerfoot.motor;

import java.util.Objects;

/**
 * Lesão sorteada na partida (SPEC §7.5).
 *
 * @param jogos partidas do clube que o jogador fica fora, a partir da seguinte
 * @param perdaForca pontos de força perdidos de vez (quem aplica respeita a força mínima)
 */
public record Lesao(GravidadeLesao gravidade, int jogos, int perdaForca) {

	public Lesao {
		Objects.requireNonNull(gravidade, "gravidade é obrigatória");
		if (jogos <= 0 || perdaForca < 0) {
			throw new IllegalArgumentException("jogos deve ser positivo e perdaForca não pode ser negativa");
		}
	}
}
