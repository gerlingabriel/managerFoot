package br.com.managerfoot.dominio;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Jogador no mínimo necessário ao motor de partidas (Etapa 1) e ao molde (Etapa 3b.2).
 * Contrato entra na etapa de economia.
 *
 * @param id identificador único dentro do jogo; distingue jogadores com dados iguais
 * @param asterisco jogador especial (SPEC §5.3): mais chance de finalizar e de marcar, e desempata o
 *        batedor de pênalti; inclui o asterisco temporário do bônus de artilharia (SPEC §5.2)
 * @param nacionalidade código de país com 3 letras maiúsculas, o mesmo usado nos países do molde
 *        (ex.: BRA); pode ser de um país sem liga cadastrada. Quem conta como estrangeiro fica para
 *        a regra da SPEC §8.4.
 */
public record Jogador(long id, String nome, Posicao posicao, int forca, Temperamento temperamento,
		boolean asterisco, String nacionalidade) {

	/** Escala de força definida na SPEC §4.2. */
	public static final int FORCA_MINIMA = 1;
	public static final int FORCA_MAXIMA = 50;

	private static final Pattern CODIGO_PAIS = Pattern.compile("[A-Z]{3}");

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
		if (nacionalidade == null || !CODIGO_PAIS.matcher(nacionalidade).matches()) {
			throw new IllegalArgumentException("nacionalidade de " + nome
					+ " deve ser um código de país com 3 letras maiúsculas, mas foi " + nacionalidade);
		}
	}

	/** Jogador sem asterisco. */
	public Jogador(long id, String nome, Posicao posicao, int forca, Temperamento temperamento, String nacionalidade) {
		this(id, nome, posicao, forca, temperamento, false, nacionalidade);
	}

	/** O mesmo jogador com ou sem asterisco. */
	public Jogador comAsterisco(boolean asterisco) {
		return new Jogador(id, nome, posicao, forca, temperamento, asterisco, nacionalidade);
	}
}
