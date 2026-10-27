package br.com.managerfoot.temporada;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.random.RandomGenerator;

import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;

/**
 * Bônus de artilharia de uma divisão na temporada (SPEC §5.2). Os sorteados sem asterisco ganham o
 * asterisco temporário; os que já têm asterisco ganham um bônus extra de finalização, que soma ao
 * do asterisco. Vale só na temporada: na seguinte há novo sorteio.
 *
 * @param asteriscoTemporario ids dos sorteados que não tinham asterisco
 * @param bonusExtra ids dos sorteados que já tinham asterisco
 */
public record BonusArtilharia(Set<Long> asteriscoTemporario, Set<Long> bonusExtra) {

	/** Jogadores sorteados por divisão (SPEC §5.2). */
	public static final int QUANTIDADE_POR_DIVISAO = 15;
	static final int PESO_COM_ASTERISCO = 2;
	static final int PESO_SEM_ASTERISCO = 1;

	public BonusArtilharia {
		asteriscoTemporario = Set.copyOf(Objects.requireNonNull(asteriscoTemporario));
		bonusExtra = Set.copyOf(Objects.requireNonNull(bonusExtra));
	}

	/**
	 * Sorteia, sem repetição, entre defensores, meias e atacantes da divisão; quem tem asterisco tem
	 * peso 2. Com menos elegíveis que {@code quantidade}, todos são sorteados.
	 */
	public static BonusArtilharia sortear(List<Jogador> jogadoresDaDivisao, int quantidade, RandomGenerator aleatorio) {
		if (quantidade < 0) {
			throw new IllegalArgumentException("a quantidade de sorteados não pode ser negativa: " + quantidade);
		}
		List<Jogador> elegiveis = new ArrayList<>(
				jogadoresDaDivisao.stream().filter(j -> j.posicao() != Posicao.GOLEIRO).toList());
		Set<Long> temporario = new HashSet<>();
		Set<Long> extra = new HashSet<>();
		while (temporario.size() + extra.size() < quantidade && !elegiveis.isEmpty()) {
			Jogador sorteado = elegiveis.remove(indicePonderado(elegiveis, aleatorio));
			(sorteado.asterisco() ? extra : temporario).add(sorteado.id());
		}
		return new BonusArtilharia(temporario, extra);
	}

	private static int indicePonderado(List<Jogador> jogadores, RandomGenerator aleatorio) {
		int total = jogadores.stream().mapToInt(BonusArtilharia::peso).sum();
		int alvo = aleatorio.nextInt(total);
		for (int i = 0; i < jogadores.size(); i++) {
			alvo -= peso(jogadores.get(i));
			if (alvo < 0) {
				return i;
			}
		}
		throw new IllegalStateException("sorteio ponderado sem resultado");
	}

	private static int peso(Jogador jogador) {
		return jogador.asterisco() ? PESO_COM_ASTERISCO : PESO_SEM_ASTERISCO;
	}

	/** O jogador como entra em campo na temporada: com o asterisco temporário, se sorteado. */
	public Jogador aplicar(Jogador jogador) {
		if (!asteriscoTemporario.contains(jogador.id())) {
			return jogador;
		}
		return new Jogador(jogador.id(), jogador.nome(), jogador.posicao(), jogador.forca(), jogador.temperamento(), true);
	}
}
