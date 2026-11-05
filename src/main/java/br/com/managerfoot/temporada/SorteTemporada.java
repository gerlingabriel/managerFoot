package br.com.managerfoot.temporada;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.dominio.Sorte;

/**
 * Beneficiados de uma divisão na temporada (SPEC §5.3.1, rev. 108), ocultos dos participantes: dois clubes da máquina
 * sorteados e todo clube de usuário com força média do plantel abaixo de 40 no início da temporada. Clube de usuário
 * não entra no sorteio. Não há mais prejudicado.
 */
public record SorteTemporada(Set<Clube> beneficiados) {

	public SorteTemporada {
		beneficiados = Set.copyOf(beneficiados);
	}

	/**
	 * Sorteia os beneficiados da máquina, com chance igual para todos, e junta os clubes de usuário fracos.
	 *
	 * @param deUsuario ids dos clubes que têm treinador usuário no início da temporada
	 * @param planteis jogadores de cada clube; clube ausente conta como sem jogadores (fica sem o benefício)
	 */
	public static SorteTemporada sortear(List<Clube> clubes, Set<Long> deUsuario, Map<Long, List<Jogador>> planteis,
			ParametrosSorteTemporada parametros, RandomGenerator aleatorio) {
		Set<Clube> beneficiados = new LinkedHashSet<>();
		List<Clube> daMaquina = new ArrayList<>(clubes.stream().filter(c -> !deUsuario.contains(c.id())).toList());
		for (int i = 0; i < parametros.sorteados() && !daMaquina.isEmpty(); i++) {
			beneficiados.add(daMaquina.remove(aleatorio.nextInt(daMaquina.size())));
		}
		clubes.stream()
			.filter(c -> deUsuario.contains(c.id()))
			.filter(c -> forcaMedia(planteis.getOrDefault(c.id(), List.of())) < parametros.forcaMediaLimiteDoUsuario())
			.forEach(beneficiados::add);
		return new SorteTemporada(beneficiados);
	}

	/** Força média do plantel inteiro; sem jogadores, infinita (não dá o benefício). */
	static double forcaMedia(List<Jogador> plantel) {
		return plantel.stream().mapToInt(Jogador::forca).average().orElse(Double.POSITIVE_INFINITY);
	}

	public Sorte de(Clube clube) {
		return beneficiados.contains(clube) ? Sorte.BENEFICIADO : Sorte.NEUTRA;
	}
}
