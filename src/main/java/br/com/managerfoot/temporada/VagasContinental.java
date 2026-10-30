package br.com.managerfoot.temporada;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.random.RandomGenerator;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;

/**
 * Quem ocupa as vagas de cada país na competição continental (SPEC §8.3.2, §8.3.3 e §8.3.6). Um país
 * com menos clubes do que vagas leva só os que tem; as vagas que sobram não passam a outro país.
 */
public final class VagasContinental {

	private VagasContinental() {
	}

	/**
	 * O país jogado, pela temporada anterior (rev. 72): os primeiros da 1ª divisão, com o campeão da
	 * copa ocupando uma das vagas; se ele já entrou pela liga, a vaga vai ao seguinte da liga.
	 *
	 * @param primeiraDivisao a classificação final da 1ª divisão, do 1º ao último
	 * @param campeaoCopa vazio se não houve copa
	 */
	public static List<Clube> peloDesempenho(List<Clube> primeiraDivisao, Optional<Clube> campeaoCopa, int vagas) {
		if (vagas < 0) {
			throw new IllegalArgumentException("vagas não podem ser negativas: " + vagas);
		}
		Set<Clube> escolhidos = new LinkedHashSet<>();
		campeaoCopa.filter(c -> vagas > 0).ifPresent(escolhidos::add);
		for (Clube clube : primeiraDivisao) {
			if (escolhidos.size() >= vagas) {
				break;
			}
			escolhidos.add(clube);
		}
		return List.copyOf(escolhidos);
	}

	/**
	 * Os clubes de um país com liga que não é jogada e que concorrem às vagas (rev. 74): os mais fortes,
	 * no tamanho da 1ª divisão dele; empatados, o de menor id.
	 */
	public static List<Clube> elegiveisDoPaisNaoJogado(List<Clube> clubes, int clubesDaPrimeiraDivisao) {
		return clubes.stream()
			.sorted(java.util.Comparator.comparingInt(Clube::forca).reversed().thenComparingLong(Clube::id))
			.limit(clubesDaPrimeiraDivisao)
			.toList();
	}

	/**
	 * Por força com sorteio (§8.3.2 item 2): sorteio sem reposição em que cada clube pesa a força em
	 * campo elevada a {@code expoente}, então os fortes entram mais, mas não sempre.
	 */
	public static List<Clube> porForca(List<Clube> clubes, Map<Long, List<Jogador>> planteis, int vagas, double expoente,
			RandomGenerator aleatorio) {
		if (vagas < 0) {
			throw new IllegalArgumentException("vagas não podem ser negativas: " + vagas);
		}
		List<Clube> restantes = new ArrayList<>(clubes);
		List<Double> pesos = new ArrayList<>();
		for (Clube clube : restantes) {
			pesos.add(Math.pow(Math.max(1.0, Clube.forcaEmCampo(planteis.getOrDefault(clube.id(), List.of()))), expoente));
		}
		List<Clube> escolhidos = new ArrayList<>();
		while (escolhidos.size() < vagas && !restantes.isEmpty()) {
			double total = pesos.stream().mapToDouble(Double::doubleValue).sum();
			double sorteio = aleatorio.nextDouble() * total;
			int i = 0;
			while (i < restantes.size() - 1 && sorteio >= pesos.get(i)) {
				sorteio -= pesos.get(i);
				i++;
			}
			escolhidos.add(restantes.remove(i));
			pesos.remove(i);
		}
		return List.copyOf(escolhidos);
	}
}
