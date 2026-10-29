package br.com.managerfoot.temporada;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Tamanho e quadro de rodadas da copa nacional (SPEC §8.2, rev. 69).
 *
 * @param clubesComUmaDivisao clubes da copa numa liga de 1 divisão
 * @param clubesComDuasOuMais clubes da copa numa liga de 2 divisões ou mais
 * @param rodadas para cada fase de ida e volta, depois de qual rodada da liga se jogam a ida e a
 *        volta; a final é depois da última rodada da liga
 */
public record ParametrosCopa(int clubesComUmaDivisao, int clubesComDuasOuMais, Map<FaseCopa, Pernas> rodadas) {

	/** Rodadas da liga depois das quais se jogam a ida e a volta. */
	public record Pernas(int ida, int volta) {

		public Pernas {
			if (ida < 1 || volta <= ida) {
				throw new IllegalArgumentException("a ida vem depois da rodada 1 ou mais e antes da volta: " + ida + " e " + volta);
			}
		}
	}

	public ParametrosCopa {
		potenciaDeDois(clubesComUmaDivisao);
		potenciaDeDois(clubesComDuasOuMais);
		rodadas = Map.copyOf(Objects.requireNonNull(rodadas));
		int anterior = 0;
		for (FaseCopa fase : FaseCopa.values()) {
			if (fase.jogoUnico()) {
				continue;
			}
			Pernas pernas = rodadas.get(fase);
			if (pernas == null) {
				throw new IllegalArgumentException("falta o quadro de rodadas da fase " + fase.nome());
			}
			if (pernas.ida() <= anterior) {
				throw new IllegalArgumentException("a fase " + fase.nome() + " começa antes de a anterior terminar");
			}
			anterior = pernas.volta();
		}
	}

	private static void potenciaDeDois(int clubes) {
		FaseCopa.comClubes(clubes);
	}

	/** Quadro definido pelo usuário: 16 avos 3/5, oitavas 10/12, quartas 18/20, semifinal 26/28. */
	public static ParametrosCopa padrao() {
		Map<FaseCopa, Pernas> rodadas = new EnumMap<>(FaseCopa.class);
		rodadas.put(FaseCopa.DEZESSEIS_AVOS, new Pernas(3, 5));
		rodadas.put(FaseCopa.OITAVAS, new Pernas(10, 12));
		rodadas.put(FaseCopa.QUARTAS, new Pernas(18, 20));
		rodadas.put(FaseCopa.SEMIFINAL, new Pernas(26, 28));
		return new ParametrosCopa(16, 32, rodadas);
	}

	/** Tamanho da copa pelo número de divisões, antes de olhar quantos clubes existem. */
	public int clubes(int divisoes) {
		if (divisoes < 1) {
			throw new IllegalArgumentException("a liga precisa de ao menos 1 divisão, mas tem " + divisoes);
		}
		return divisoes == 1 ? clubesComUmaDivisao : clubesComDuasOuMais;
	}

	public Pernas pernas(FaseCopa fase) {
		if (fase.jogoUnico()) {
			throw new IllegalArgumentException("a final é jogo único, depois da última rodada da liga");
		}
		return rodadas.get(fase);
	}
}
