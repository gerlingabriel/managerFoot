package br.com.managerfoot.temporada;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Formato e quadro de rodadas da competição continental (SPEC §8.3.6, rev. 72 e 73).
 *
 * @param grupos quantos grupos
 * @param clubesPorGrupo clubes em cada grupo, um de cada pote; por enquanto só 4
 * @param rodadasDosGrupos depois de qual rodada da liga se joga cada jogo dos grupos (turno e returno)
 * @param mataMata ida e volta das oitavas à semifinal; a final é depois da final da copa
 * @param expoenteForca peso da força na escolha por força com sorteio (§8.3.2 item 2): o clube pesa a
 *        força em campo elevada a este número (provisório)
 */
public record ParametrosContinental(int grupos, int clubesPorGrupo, List<Integer> rodadasDosGrupos,
		Map<FaseCopa, ParametrosCopa.Pernas> mataMata, double expoenteForca) {

	/** Passam os 2 primeiros de cada grupo (rev. 72). */
	public static final int CLASSIFICADOS_POR_GRUPO = 2;

	public ParametrosContinental {
		rodadasDosGrupos = List.copyOf(rodadasDosGrupos);
		mataMata = Map.copyOf(Objects.requireNonNull(mataMata));
		if (grupos < 1 || clubesPorGrupo != 4) {
			throw new IllegalArgumentException("formato de grupos inválido: " + grupos + " grupos de " + clubesPorGrupo);
		}
		if (rodadasDosGrupos.size() != 2 * (clubesPorGrupo - 1)) {
			throw new IllegalArgumentException("turno e returno com " + clubesPorGrupo + " clubes tem "
					+ 2 * (clubesPorGrupo - 1) + " jogos, mas o quadro tem " + rodadasDosGrupos.size());
		}
		for (int i = 1; i < rodadasDosGrupos.size(); i++) {
			if (rodadasDosGrupos.get(i) <= rodadasDosGrupos.get(i - 1)) {
				throw new IllegalArgumentException("os jogos dos grupos precisam estar em rodadas crescentes: " + rodadasDosGrupos);
			}
		}
		int anterior = rodadasDosGrupos.getLast();
		for (FaseCopa fase = primeiraFase(grupos); !fase.jogoUnico(); fase = fase.seguinte().orElseThrow()) {
			ParametrosCopa.Pernas pernas = mataMata.get(fase);
			if (pernas == null) {
				throw new IllegalArgumentException("falta o quadro de rodadas da fase " + fase.nome());
			}
			if (pernas.ida() <= anterior) {
				throw new IllegalArgumentException("a fase " + fase.nome() + " começa antes de a anterior terminar");
			}
			anterior = pernas.volta();
		}
		if (expoenteForca < 0) {
			throw new IllegalArgumentException("o expoente da força não pode ser negativo: " + expoenteForca);
		}
	}

	private static FaseCopa primeiraFase(int grupos) {
		return FaseCopa.comClubes(grupos * CLASSIFICADOS_POR_GRUPO);
	}

	/** Primeira fase do mata-mata: as oitavas com 8 grupos. */
	public FaseCopa primeiraFase() {
		return primeiraFase(grupos);
	}

	public int clubes() {
		return grupos * clubesPorGrupo;
	}

	/** Quadro da rev. 72: grupos 2, 4, 7, 9, 14, 16; oitavas 19/21, quartas 22/24, semifinal 25/27. */
	public static ParametrosContinental padrao() {
		Map<FaseCopa, ParametrosCopa.Pernas> mataMata = new EnumMap<>(FaseCopa.class);
		mataMata.put(FaseCopa.OITAVAS, new ParametrosCopa.Pernas(19, 21));
		mataMata.put(FaseCopa.QUARTAS, new ParametrosCopa.Pernas(22, 24));
		mataMata.put(FaseCopa.SEMIFINAL, new ParametrosCopa.Pernas(25, 27));
		return new ParametrosContinental(8, 4, List.of(2, 4, 7, 9, 14, 16), mataMata, 3.0);
	}
}
