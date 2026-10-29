package br.com.managerfoot.temporada;

import java.util.Arrays;
import java.util.Optional;

/** Fases da copa nacional (SPEC §8.2), da primeira à final. */
public enum FaseCopa {
	DEZESSEIS_AVOS(32, "16 avos de final"),
	OITAVAS(16, "Oitavas de final"),
	QUARTAS(8, "Quartas de final"),
	SEMIFINAL(4, "Semifinal"),
	FINAL(2, "Final");

	private final int clubes;
	private final String nome;

	FaseCopa(int clubes, String nome) {
		this.clubes = clubes;
		this.nome = nome;
	}

	/** Clubes que disputam a fase. */
	public int clubes() {
		return clubes;
	}

	public String nome() {
		return nome;
	}

	/** A final é jogo único em campo neutro; as outras fases, ida e volta. */
	public boolean jogoUnico() {
		return this == FINAL;
	}

	public Optional<FaseCopa> seguinte() {
		return this == FINAL ? Optional.empty() : Optional.of(values()[ordinal() + 1]);
	}

	/** A fase que começa com este número de clubes. */
	public static FaseCopa comClubes(int clubes) {
		return Arrays.stream(values())
			.filter(f -> f.clubes == clubes)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("nenhuma fase da copa tem " + clubes + " clubes"));
	}
}
