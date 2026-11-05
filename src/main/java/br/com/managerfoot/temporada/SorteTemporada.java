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
 * Beneficiados e prejudicados de uma divisão na temporada (SPEC §5.3.1, rev. 108 e 109), ocultos dos participantes:
 * dois beneficiados sorteados entre todos os clubes, inclusive os de usuário, mais todo clube de usuário com força
 * média do plantel abaixo de 40 no início da temporada; um prejudicado sorteado só entre os clubes da máquina.
 */
public record SorteTemporada(Set<Clube> beneficiados, Set<Clube> prejudicados) {

	public SorteTemporada {
		beneficiados = Set.copyOf(beneficiados);
		prejudicados = Set.copyOf(prejudicados);
		if (beneficiados.stream().anyMatch(prejudicados::contains)) {
			throw new IllegalArgumentException("o mesmo clube não pode ser beneficiado e prejudicado");
		}
	}

	/**
	 * Sorteia os beneficiados entre todos e os prejudicados entre os da máquina que sobraram, com chance igual para
	 * todos, e junta os clubes de usuário fracos aos beneficiados.
	 *
	 * @param deUsuario ids dos clubes que têm treinador usuário no início da temporada
	 * @param planteis jogadores de cada clube; clube ausente conta como sem jogadores (fica sem o benefício)
	 */
	public static SorteTemporada sortear(List<Clube> clubes, Set<Long> deUsuario, Map<Long, List<Jogador>> planteis,
			ParametrosSorteTemporada parametros, RandomGenerator aleatorio) {
		List<Clube> restantes = new ArrayList<>(clubes);
		Set<Clube> beneficiados = new LinkedHashSet<>();
		for (int i = 0; i < parametros.beneficiados() && !restantes.isEmpty(); i++) {
			beneficiados.add(restantes.remove(aleatorio.nextInt(restantes.size())));
		}
		List<Clube> daMaquina = new ArrayList<>(restantes.stream().filter(c -> !deUsuario.contains(c.id())).toList());
		Set<Clube> prejudicados = new LinkedHashSet<>();
		for (int i = 0; i < parametros.prejudicados() && !daMaquina.isEmpty(); i++) {
			prejudicados.add(daMaquina.remove(aleatorio.nextInt(daMaquina.size())));
		}
		clubes.stream()
			.filter(c -> deUsuario.contains(c.id()))
			.filter(c -> forcaMedia(planteis.getOrDefault(c.id(), List.of())) < parametros.forcaMediaLimiteDoUsuario())
			.forEach(beneficiados::add);
		return new SorteTemporada(beneficiados, prejudicados);
	}

	/** Força média do plantel inteiro; sem jogadores, infinita (não dá o benefício). */
	static double forcaMedia(List<Jogador> plantel) {
		return plantel.stream().mapToInt(Jogador::forca).average().orElse(Double.POSITIVE_INFINITY);
	}

	public Sorte de(Clube clube) {
		if (beneficiados.contains(clube)) {
			return Sorte.BENEFICIADO;
		}
		return prejudicados.contains(clube) ? Sorte.PREJUDICADO : Sorte.NEUTRA;
	}
}
