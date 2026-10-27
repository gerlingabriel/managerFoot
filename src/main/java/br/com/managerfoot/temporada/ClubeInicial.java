package br.com.managerfoot.temporada;

import java.util.Comparator;
import java.util.List;
import java.util.random.RandomGenerator;

import br.com.managerfoot.dominio.Clube;

/**
 * Sorteio do clube inicial (SPEC §3.2, rev. 15): entre os clubes de menor força da última divisão.
 * Quem empata com o último do grupo também entra, para o desempate não depender da ordem de
 * cadastro; uma divisão menor que o grupo sorteia entre todos os seus clubes.
 */
public final class ClubeInicial {

	private ClubeInicial() {
	}

	/**
	 * Clubes que podem ser sorteados, em ordem de id.
	 *
	 * @param quantidade tamanho do grupo dos mais fracos (provisório: 4)
	 */
	public static List<Clube> candidatos(Divisao ultima, int quantidade) {
		if (quantidade < 1) {
			throw new IllegalArgumentException("o grupo do sorteio do clube inicial precisa de ao menos 1 clube");
		}
		List<Clube> clubes = ultima.clubes();
		if (clubes.isEmpty()) {
			throw new IllegalArgumentException("\"" + ultima.nome() + "\" não tem clubes para sortear");
		}
		List<Integer> forcas = clubes.stream().map(Clube::forca).sorted().toList();
		int corte = forcas.get(Math.min(quantidade, forcas.size()) - 1);
		return clubes.stream().filter(c -> c.forca() <= corte).sorted(Comparator.comparingLong(Clube::id)).toList();
	}

	public static Clube sortear(Divisao ultima, int quantidade, RandomGenerator aleatorio) {
		List<Clube> candidatos = candidatos(ultima, quantidade);
		return candidatos.get(aleatorio.nextInt(candidatos.size()));
	}
}
