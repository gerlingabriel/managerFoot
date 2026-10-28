package br.com.managerfoot.economia;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Resolução de um leilão (SPEC §9.4, rev. 41 e 42): cada participante dá um lance; leva o maior, o
 * empate é sorteado e lance abaixo do mínimo não vale. Sem lance válido o jogador não é vendido.
 */
public final class Leilao {

	private Leilao() {
	}

	/**
	 * @param lanceMinimo lance inicial (zero no leilão sem lance mínimo)
	 * @param aleatorio sorteio do empate
	 */
	public static Optional<Lance> vencedor(List<Lance> lances, long lanceMinimo, RandomGenerator aleatorio) {
		List<Lance> validos = lances.stream().filter(l -> l.valor() >= lanceMinimo).toList();
		if (validos.isEmpty()) {
			return Optional.empty();
		}
		long maior = validos.stream().mapToLong(Lance::valor).max().orElseThrow();
		// ordem fixa antes do sorteio, para a mesma semente dar o mesmo vencedor
		List<Lance> empatados = validos.stream().filter(l -> l.valor() == maior)
			.sorted(Comparator.comparingLong(Lance::clubeId)).toList();
		return Optional.of(empatados.get(aleatorio.nextInt(empatados.size())));
	}

	/** Lance de um clube, em centavos de Real. */
	public record Lance(long clubeId, long valor) {

		public Lance {
			if (valor < 0) {
				throw new IllegalArgumentException("lance negativo: " + valor);
			}
		}
	}
}
