package br.com.managerfoot.dominio;

/** Posição de origem do jogador. A escalação deve respeitá-la (SPEC §7.2). */
public enum Posicao {
	GOLEIRO("goleiro", "goleiros"),
	DEFENSOR("defensor", "defensores"),
	MEIO_CAMPISTA("meio-campista", "meio-campistas"),
	ATACANTE("atacante", "atacantes");

	private final String singular;
	private final String plural;

	Posicao(String singular, String plural) {
		this.singular = singular;
		this.plural = plural;
	}

	/** Nome legível para mensagens, no singular ou plural conforme a quantidade. */
	public String nome(int quantidade) {
		return quantidade == 1 ? singular : plural;
	}
}
