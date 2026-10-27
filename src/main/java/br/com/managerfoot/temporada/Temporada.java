package br.com.managerfoot.temporada;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SplittableRandom;

import br.com.managerfoot.dominio.Jogador;

/**
 * Temporada da liga com os sorteios feitos no seu início: em cada divisão, o calendário
 * (SPEC §8.1), a sorte e o azar (§5.3.1) e o bônus de artilharia (§5.2). Mesma semente, mesmos
 * sorteios. A semente também é a do desempate por sorteio da classificação (§8.1, rev. 12).
 *
 * @param numero 1 na primeira temporada do jogo
 * @param divisoes uma por divisão da liga, em ordem de prioridade
 */
public record Temporada(int numero, long semente, List<SorteiosDivisao> divisoes) {

	/** Sorteios de uma divisão na temporada. */
	public record SorteiosDivisao(int prioridade, Calendario calendario, SorteTemporada sorte, BonusArtilharia bonus) {

		public SorteiosDivisao {
			Objects.requireNonNull(calendario, "calendário é obrigatório");
			Objects.requireNonNull(sorte, "sorte é obrigatória");
			Objects.requireNonNull(bonus, "bônus de artilharia é obrigatório");
		}
	}

	public Temporada {
		if (numero < 1) {
			throw new IllegalArgumentException("o número da temporada começa em 1, mas foi " + numero);
		}
		divisoes = List.copyOf(divisoes);
	}

	/**
	 * Faz os sorteios do início da temporada. Cada divisão usa um gerador próprio derivado da
	 * semente, na ordem de prioridade; dentro dela, sorteia calendário, sorte e bônus, nessa ordem.
	 *
	 * @param planteis jogadores de cada clube, por id do clube; clube ausente conta como sem jogadores
	 */
	public static Temporada iniciar(int numero, ConfiguracaoLiga liga, Map<Long, List<Jogador>> planteis,
			long semente) {
		SplittableRandom aleatorio = new SplittableRandom(semente);
		List<SorteiosDivisao> divisoes = new ArrayList<>();
		for (Divisao divisao : liga.divisoes()) {
			SplittableRandom daDivisao = aleatorio.split();
			List<Jogador> jogadores = divisao.clubes()
				.stream()
				.flatMap(c -> planteis.getOrDefault(c.id(), List.of()).stream())
				.toList();
			Calendario calendario = Calendario.sorteado(divisao.clubes(), daDivisao);
			SorteTemporada sorte = SorteTemporada.sortear(divisao.clubes(), daDivisao);
			BonusArtilharia bonus = BonusArtilharia.sortear(jogadores, BonusArtilharia.QUANTIDADE_POR_DIVISAO, daDivisao);
			divisoes.add(new SorteiosDivisao(divisao.prioridade(), calendario, sorte, bonus));
		}
		return new Temporada(numero, semente, divisoes);
	}
}
