package br.com.managerfoot.motor;

/**
 * Partida que precisa de um vencedor (SPEC §8.2): a volta do ida e volta ou a final única. Com o
 * placar somado empatado no fim dos 90 minutos há prorrogação e, persistindo o empate, disputa de
 * pênaltis. Gol fora de casa não vale (rev. 70).
 *
 * @param golsCasaAntes gols que o mandante desta partida já marcou no confronto (na ida); 0 na final
 * @param golsForaAntes gols que o visitante desta partida já marcou no confronto
 * @param campoNeutro sem a vantagem do mando (final única)
 */
public record JogoDecisivo(int golsCasaAntes, int golsForaAntes, boolean campoNeutro) {

	/** Duração da prorrogação, dois tempos de 15 minutos. */
	public static final int MINUTOS_PRORROGACAO = 30;

	/** Final única em campo neutro. */
	public static final JogoDecisivo FINAL = new JogoDecisivo(0, 0, true);

	public JogoDecisivo {
		if (golsCasaAntes < 0 || golsForaAntes < 0) {
			throw new IllegalArgumentException("gols não podem ser negativos: " + golsCasaAntes + "x" + golsForaAntes);
		}
	}

	/** Volta do ida e volta, com os gols que cada lado desta partida marcou na ida. */
	public static JogoDecisivo volta(int golsCasaNaIda, int golsForaNaIda) {
		return new JogoDecisivo(golsCasaNaIda, golsForaNaIda, false);
	}
}
