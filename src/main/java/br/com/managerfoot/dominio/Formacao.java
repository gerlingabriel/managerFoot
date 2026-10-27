package br.com.managerfoot.dominio;

import java.util.Arrays;
import java.util.Optional;

/**
 * Formações permitidas (SPEC §7.2). Toda formação tem 1 goleiro e 10 jogadores de linha.
 * Classificação de estilo é provisória e pode mudar após a simulação.
 */
public enum Formacao {
	F_4_4_2(4, 4, 2, EstiloTatico.NORMAL),
	F_3_5_2(3, 5, 2, EstiloTatico.NORMAL),
	F_4_3_3(4, 3, 3, EstiloTatico.OFENSIVO),
	F_3_4_3(3, 4, 3, EstiloTatico.OFENSIVO),
	F_4_2_4(4, 2, 4, EstiloTatico.OFENSIVO),
	F_5_4_1(5, 4, 1, EstiloTatico.DEFENSIVO),
	F_4_5_1(4, 5, 1, EstiloTatico.DEFENSIVO),
	F_3_6_1(3, 6, 1, EstiloTatico.DEFENSIVO);

	public static final int TITULARES = 11;

	private final int defensores;
	private final int meioCampistas;
	private final int atacantes;
	private final EstiloTatico estilo;

	Formacao(int defensores, int meioCampistas, int atacantes, EstiloTatico estilo) {
		this.defensores = defensores;
		this.meioCampistas = meioCampistas;
		this.atacantes = atacantes;
		this.estilo = estilo;
	}

	public int quantidade(Posicao posicao) {
		return switch (posicao) {
			case GOLEIRO -> 1;
			case DEFENSOR -> defensores;
			case MEIO_CAMPISTA -> meioCampistas;
			case ATACANTE -> atacantes;
		};
	}

	public EstiloTatico estilo() {
		return estilo;
	}

	/** Rótulo exibido ao usuário, por exemplo "4-4-2". */
	public String rotulo() {
		return defensores + "-" + meioCampistas + "-" + atacantes;
	}

	public static Optional<Formacao> porRotulo(String rotulo) {
		return Arrays.stream(values()).filter(f -> f.rotulo().equals(rotulo)).findFirst();
	}
}
