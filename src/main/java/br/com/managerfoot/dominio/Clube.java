package br.com.managerfoot.dominio;

import java.util.Objects;

/**
 * Clube no mínimo necessário à estrutura da liga (Etapa 2). Estádio, país e finanças entram nas
 * etapas seguintes.
 *
 * @param forca força cadastrada (SPEC §4.3), na escala de {@link Jogador#FORCA_MINIMA} a
 *        {@link Jogador#FORCA_MAXIMA}
 * @param escudo modelo e cores (SPEC §10.2)
 */
public record Clube(long id, String nome, int forca, Escudo escudo) {

	/** Máximo de jogadores no plantel (SPEC §5.2): os 23 gerados mais 2 vagas para o mercado. */
	public static final int MAXIMO_JOGADORES = 25;

	public Clube {
		Objects.requireNonNull(escudo, "escudo do clube é obrigatório");
		if (nome == null || nome.isBlank()) {
			throw new IllegalArgumentException("nome do clube é obrigatório");
		}
		if (forca < Jogador.FORCA_MINIMA || forca > Jogador.FORCA_MAXIMA) {
			throw new IllegalArgumentException("força de " + nome + " deve estar entre " + Jogador.FORCA_MINIMA + " e "
					+ Jogador.FORCA_MAXIMA + ", mas foi " + forca);
		}
	}

	/** Clube ainda sem escudo cadastrado: usa {@link Escudo#NEUTRO}. */
	public Clube(long id, String nome, int forca) {
		this(id, nome, forca, Escudo.NEUTRO);
	}
}
