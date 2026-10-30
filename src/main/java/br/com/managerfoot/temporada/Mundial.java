package br.com.managerfoot.temporada;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.random.RandomGenerator;

import br.com.managerfoot.bancopadrao.Regiao;
import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;

/**
 * Mundial de Clubes da temporada (SPEC §8.3.4, rev. 88): quatro clubes, depois da final da continental. Uma
 * semifinal é da Europa contra um clube sorteado entre Ásia, África e Oceania; a outra, da América do Norte
 * contra a América do Sul. Semifinais e final em jogo único, com prorrogação e pênaltis. Domínio puro: quem
 * chama joga as partidas e registra os placares.
 *
 * @param semifinais a da Europa e depois a das Américas
 * @param decisao criada quando as semifinais terminam
 */
public record Mundial(List<Confronto> semifinais, Optional<Confronto> decisao) {

	/** As etapas, nesta ordem: as duas semifinais juntas e a final. */
	public static final List<FaseCopa> ETAPAS = List.of(FaseCopa.SEMIFINAL, FaseCopa.FINAL);

	/** De onde sai o adversário da Europa na semifinal. */
	private static final List<Regiao> REGIOES_SORTEADAS = List.of(Regiao.ASIA, Regiao.AFRICA, Regiao.OCEANIA);

	/**
	 * Um jogo do Mundial, em campo neutro.
	 *
	 * @param primeiro só o primeiro nome do jogo: não há mando
	 * @param placar visto do {@code primeiro}; vazio até ser jogado
	 */
	public record Confronto(FaseCopa fase, int ordem, Clube primeiro, Clube segundo, Optional<PlacarCopa> placar) {

		public Confronto {
			Objects.requireNonNull(fase);
			Objects.requireNonNull(primeiro);
			Objects.requireNonNull(segundo);
			Objects.requireNonNull(placar);
			if (primeiro.equals(segundo)) {
				throw new IllegalArgumentException(primeiro.nome() + " não pode enfrentar a si mesmo");
			}
			placar.ifPresent(p -> {
				if (p.golsMandante() == p.golsVisitante() && p.penaltis().isEmpty()) {
					throw new IllegalArgumentException("jogo do Mundial empatado precisa de pênaltis");
				}
			});
		}

		public Optional<Clube> vencedor() {
			return placar.map(p -> {
				if (p.golsMandante() != p.golsVisitante()) {
					return p.golsMandante() > p.golsVisitante() ? primeiro : segundo;
				}
				PlacarCopa.Penaltis penaltis = p.penaltis().orElseThrow();
				return penaltis.mandante() > penaltis.visitante() ? primeiro : segundo;
			});
		}

		public Optional<Clube> perdedor() {
			return vencedor().map(v -> v.equals(primeiro) ? segundo : primeiro);
		}

		public boolean envolve(Clube clube) {
			return primeiro.equals(clube) || segundo.equals(clube);
		}

		Confronto comPlacar(PlacarCopa novo) {
			if (placar.isPresent()) {
				throw new IllegalStateException(primeiro.nome() + " x " + segundo.nome() + " já foi jogado");
			}
			return new Confronto(fase, ordem, primeiro, segundo, Optional.of(novo));
		}
	}

	public Mundial {
		semifinais = List.copyOf(semifinais);
		Objects.requireNonNull(decisao);
		if (semifinais.size() != 2) {
			throw new IllegalArgumentException("o Mundial tem duas semifinais, não " + semifinais.size());
		}
	}

	/**
	 * Monta as semifinais. A região do campeão continental é representada por ele; as outras, por um clube
	 * escolhido por força com sorteio (§8.3.2 item 2).
	 *
	 * @param candidatos os clubes que podem representar cada região
	 * @return vazio se falta clube de alguma região: não se inventam participantes (§8.3.4)
	 */
	public static Optional<Mundial> sortear(Clube campeaoContinental, Regiao regiaoDoCampeao, Map<Regiao, List<Clube>> candidatos,
			Map<Long, List<Jogador>> planteis, double expoenteForca, RandomGenerator aleatorio) {
		List<Regiao> sorteaveis = REGIOES_SORTEADAS.stream().filter(r -> !candidatos.getOrDefault(r, List.of()).isEmpty()).toList();
		if (sorteaveis.isEmpty()) {
			return Optional.empty();
		}
		Regiao sorteada = sorteaveis.get(aleatorio.nextInt(sorteaveis.size()));
		List<Optional<Clube>> participantes = new ArrayList<>();
		for (Regiao regiao : List.of(Regiao.EUROPA, sorteada, Regiao.AMERICA_DO_NORTE, Regiao.AMERICA_DO_SUL)) {
			participantes.add(regiao == regiaoDoCampeao ? Optional.of(campeaoContinental)
					: VagasContinental.porForca(candidatos.getOrDefault(regiao, List.of()), planteis, 1, expoenteForca, aleatorio)
						.stream().findFirst());
		}
		if (participantes.stream().anyMatch(Optional::isEmpty)) {
			return Optional.empty();
		}
		return Optional.of(new Mundial(
				List.of(new Confronto(FaseCopa.SEMIFINAL, 0, participantes.get(0).get(), participantes.get(1).get(), Optional.empty()),
						new Confronto(FaseCopa.SEMIFINAL, 1, participantes.get(2).get(), participantes.get(3).get(), Optional.empty())),
				Optional.empty()));
	}

	/** Os jogos da etapa. A final só existe depois das semifinais. */
	public List<Confronto> confrontos(FaseCopa fase) {
		return fase == FaseCopa.FINAL ? decisao.stream().toList() : semifinais;
	}

	public Mundial comPlacar(FaseCopa fase, int ordem, PlacarCopa placar) {
		if (fase == FaseCopa.FINAL) {
			return new Mundial(semifinais, Optional.of(decisao.orElseThrow().comPlacar(placar)));
		}
		List<Confronto> novas = new ArrayList<>(semifinais);
		novas.set(ordem, novas.get(ordem).comPlacar(placar));
		return new Mundial(novas, decisao);
	}

	public boolean semifinaisEncerradas() {
		return semifinais.stream().allMatch(c -> c.placar().isPresent());
	}

	/** Cria a final com os vencedores das semifinais. */
	public Mundial comFinal() {
		if (!semifinaisEncerradas()) {
			throw new IllegalStateException("a final do Mundial espera as semifinais");
		}
		if (decisao.isPresent()) {
			return this;
		}
		return new Mundial(semifinais, Optional.of(new Confronto(FaseCopa.FINAL, 0, semifinais.get(0).vencedor().orElseThrow(),
				semifinais.get(1).vencedor().orElseThrow(), Optional.empty())));
	}

	public Optional<Clube> campeao() {
		return decisao.flatMap(Confronto::vencedor);
	}

	public Optional<Clube> vice() {
		return decisao.flatMap(Confronto::perdedor);
	}

	public boolean participa(Clube clube) {
		return semifinais.stream().anyMatch(c -> c.envolve(clube));
	}
}
