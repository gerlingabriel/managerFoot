package br.com.managerfoot.temporada;

import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Sorte;

/**
 * Beneficiado e prejudicado de uma divisão na temporada (SPEC §5.3.1). O sorteio é oculto dos
 * participantes e qualquer clube pode sair, inclusive os de usuários.
 */
public record SorteTemporada(Clube beneficiado, Clube prejudicado) {

	public SorteTemporada {
		Objects.requireNonNull(beneficiado, "beneficiado é obrigatório");
		Objects.requireNonNull(prejudicado, "prejudicado é obrigatório");
		if (beneficiado.equals(prejudicado)) {
			throw new IllegalArgumentException("o mesmo clube não pode ser beneficiado e prejudicado: " + beneficiado.nome());
		}
	}

	/** Sorteia dois clubes distintos da divisão, com chance igual para todos. */
	public static SorteTemporada sortear(List<Clube> clubes, RandomGenerator aleatorio) {
		if (clubes.size() < 2) {
			throw new IllegalArgumentException("o sorteio da sorte precisa de pelo menos 2 clubes, mas há " + clubes.size());
		}
		int b = aleatorio.nextInt(clubes.size());
		int p = aleatorio.nextInt(clubes.size() - 1);
		if (p >= b) {
			p++;
		}
		return new SorteTemporada(clubes.get(b), clubes.get(p));
	}

	public Sorte de(Clube clube) {
		if (clube.equals(beneficiado)) {
			return Sorte.BENEFICIADO;
		}
		return clube.equals(prejudicado) ? Sorte.PREJUDICADO : Sorte.NEUTRA;
	}
}
