package br.com.managerfoot.temporada;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.random.RandomGenerator;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Posicao;

/**
 * Bônus do treinador usuário na temporada (SPEC §5.3.2, rev. 121), de todos os clubes de usuário juntos. Cada
 * sorteado ganha força extra em campo (até 50) e o asterisco temporário; quem já tem asterisco ganha o bônus extra de
 * finalização, como na artilharia (§5.2). A força gravada, o salário, o preço e a evolução não mudam. Vale até o fim
 * da temporada, também se o jogador mudar de clube.
 *
 * @param sorteados por id do jogador
 */
public record BonusTreinador(Map<Long, Sorteado> sorteados) {

	/** Nenhum sorteado. */
	public static final BonusTreinador NENHUM = new BonusTreinador(Map.of());

	/**
	 * @param forcaExtra força a mais em campo
	 * @param extra já tinha asterisco: ganha o bônus extra de finalização em vez do asterisco temporário
	 */
	public record Sorteado(long jogadorId, int forcaExtra, boolean extra) {
	}

	public BonusTreinador {
		sorteados = Map.copyOf(Objects.requireNonNull(sorteados));
	}

	public static BonusTreinador de(List<Sorteado> sorteados) {
		Map<Long, Sorteado> m = new HashMap<>();
		sorteados.forEach(s -> m.put(s.jogadorId(), s));
		return new BonusTreinador(m);
	}

	/**
	 * Sorteia, com chance igual e sem repetição, entre defensores, meias e atacantes do plantel que ainda não têm o
	 * bônus; a faixa sai da força em campo do plantel. Com menos elegíveis que a faixa pede, todos são sorteados.
	 *
	 * @param jaTem jogadores que já têm o bônus na temporada (ficam fora)
	 */
	public static List<Sorteado> sortear(List<Jogador> plantel, Set<Long> jaTem, ParametrosBonusTreinador parametros,
			RandomGenerator aleatorio) {
		var faixa = parametros.faixa(Clube.forcaEmCampo(plantel));
		if (faixa.isEmpty()) {
			return List.of();
		}
		// ordem fixa antes do sorteio, para a mesma semente dar o mesmo resultado
		List<Jogador> elegiveis = new ArrayList<>(plantel.stream()
			.filter(j -> j.posicao() != Posicao.GOLEIRO && !jaTem.contains(j.id()))
			.sorted(Comparator.comparingLong(Jogador::id))
			.toList());
		List<Sorteado> sorteados = new ArrayList<>();
		while (sorteados.size() < faixa.get().jogadores() && !elegiveis.isEmpty()) {
			Jogador j = elegiveis.remove(aleatorio.nextInt(elegiveis.size()));
			sorteados.add(new Sorteado(j.id(), faixa.get().forcaExtra(), j.asterisco()));
		}
		return sorteados;
	}

	/** Ids dos sorteados que já tinham asterisco: bônus extra de finalização na partida. */
	public Set<Long> bonusExtra() {
		return Set.copyOf(sorteados.values().stream().filter(Sorteado::extra).map(Sorteado::jogadorId).toList());
	}

	/** O jogador como entra em campo: com a força extra (até 50) e, se sorteado sem asterisco, o asterisco temporário. */
	public Jogador aplicar(Jogador jogador) {
		Sorteado s = sorteados.get(jogador.id());
		if (s == null) {
			return jogador;
		}
		Jogador comForca = jogador.comForca(Math.min(Jogador.FORCA_MAXIMA, jogador.forca() + s.forcaExtra()));
		return s.extra() ? comForca : comForca.comAsterisco(true);
	}
}
