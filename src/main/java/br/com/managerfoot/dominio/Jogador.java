package br.com.managerfoot.dominio;

import java.util.Objects;

/**
 * Jogador no mínimo necessário ao motor de partidas (Etapa 1).
 * Nacionalidade e contrato entram nas etapas seguintes.
 *
 * @param id identificador único dentro do jogo; distingue jogadores com dados iguais
 * @param asterisco jogador especial (SPEC §5.3); por ora só desempata a escolha do batedor de pênalti
 */
public record Jogador(long id, String nome, Posicao posicao, int forca, Temperamento temperamento,
		boolean asterisco) {

	/** Escala de força definida na SPEC §4.2. */
	public static final int FORCA_MINIMA = 1;
	public static final int FORCA_MAXIMA = 50;

	public Jogador {
		Objects.requireNonNull(posicao, "posição do jogador é obrigatória");
		Objects.requireNonNull(temperamento, "temperamento do jogador é obrigatório");
		if (nome == null || nome.isBlank()) {
			throw new IllegalArgumentException("nome do jogador é obrigatório");
		}
		if (forca < FORCA_MINIMA || forca > FORCA_MAXIMA) {
			throw new IllegalArgumentException("força de " + nome + " deve estar entre " + FORCA_MINIMA + " e "
					+ FORCA_MAXIMA + ", mas foi " + forca);
		}
	}

	/** Jogador sem asterisco. */
	public Jogador(long id, String nome, Posicao posicao, int forca, Temperamento temperamento) {
		this(id, nome, posicao, forca, temperamento, false);
	}
}
