package br.com.managerfoot.temporada;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.random.RandomGenerator;

import br.com.managerfoot.dominio.Clube;
import br.com.managerfoot.dominio.Jogador;
import br.com.managerfoot.motor.JogoDecisivo;

/**
 * Copa nacional da temporada (SPEC §8.2): mata-mata em ida e volta com final única em campo
 * neutro. A chave é sorteada no início da temporada e fica fixa: os vencedores dos confrontos 2k e
 * 2k+1 se enfrentam na fase seguinte, com o mando da ida sorteado quando o confronto é criado.
 * Domínio puro: quem chama joga as partidas e registra os placares.
 *
 * @param confrontos de todas as fases já criadas, por fase e ordem
 */
public record Copa(List<ConfrontoCopa> confrontos) {

	/** Qual partida do confronto. */
	public enum Perna {
		IDA,
		VOLTA,
		/** A final. */
		JOGO_UNICO
	}

	/** Uma etapa do quadro: as partidas de uma fase jogadas depois de uma rodada da liga. */
	public record Etapa(int depoisDaRodada, FaseCopa fase, Perna perna) {

		/** Nome para a tela: "Oitavas de final · ida", "Final". */
		public String nome() {
			return switch (perna) {
				case IDA -> fase.nome() + " · ida";
				case VOLTA -> fase.nome() + " · volta";
				case JOGO_UNICO -> fase.nome();
			};
		}
	}

	/**
	 * Um jogo da copa visto por um clube (calendário e extrato, modelos 12d e 12f, rev. 71).
	 *
	 * @param indice posição da etapa no quadro
	 * @param emCasa nulo na final, em campo neutro
	 * @param golsPro do clube; nulo enquanto o jogo não foi jogado
	 * @param detalhe na volta, o total ("total 3 x 2 · passa", com os pênaltis) ou, antes dela, a ida
	 *        ("ida 1 x 1"); na final, os pênaltis; nulo nos outros casos
	 */
	public record JogoDoClube(int indice, Etapa etapa, Boolean emCasa, Clube adversario, Integer golsPro,
			Integer golsContra, String detalhe) {
	}

	/**
	 * Uma partida a jogar.
	 *
	 * @param jogo mandante e visitante desta partida; a rodada é a da liga depois da qual se joga
	 * @param decisivo presente na volta e na final: a partida precisa de vencedor
	 */
	public record PartidaCopa(ConfrontoCopa confronto, Perna perna, Jogo jogo, Optional<JogoDecisivo> decisivo) {
	}

	public Copa {
		confrontos = confrontos.stream()
			.sorted(Comparator.comparing(ConfrontoCopa::fase).thenComparingInt(ConfrontoCopa::ordem))
			.toList();
		if (confrontos.isEmpty()) {
			throw new IllegalArgumentException("a copa precisa de ao menos um confronto");
		}
	}

	/**
	 * Os clubes que entram na copa: o tamanho pelo número de divisões ou, com menos clubes, a maior
	 * potência de 2 que cabe (rev. 70). Ficam de fora os de menor força em campo no início da
	 * temporada; empatados na linha de corte, decide o sorteio.
	 *
	 * @param clubes os das divisões, sem a fila de espera
	 * @param planteis jogadores de cada clube, por id do clube
	 * @throws IllegalArgumentException com menos de 2 clubes
	 */
	public static List<Clube> inscritos(List<Clube> clubes, Map<Long, List<Jogador>> planteis, int divisoes,
			ParametrosCopa parametros, RandomGenerator aleatorio) {
		if (clubes.size() < 2) {
			throw new IllegalArgumentException("a copa precisa de ao menos 2 clubes, mas há " + clubes.size());
		}
		int vagas = Math.min(parametros.clubes(divisoes), Integer.highestOneBit(clubes.size()));
		List<Clube> ordem = new ArrayList<>(clubes);
		Collections.shuffle(ordem, aleatorio);
		ordem.sort(Comparator.comparingDouble(
				(Clube c) -> Clube.forcaEmCampo(planteis.getOrDefault(c.id(), List.of()))).reversed());
		return List.copyOf(ordem.subList(0, vagas));
	}

	/**
	 * Sorteia a chave e cria os confrontos da primeira fase.
	 *
	 * @param inscritos uma potência de 2, de 2 a 32
	 */
	public static Copa sortear(List<Clube> inscritos, RandomGenerator aleatorio) {
		FaseCopa primeira = FaseCopa.comClubes(inscritos.size());
		if (inscritos.stream().distinct().count() != inscritos.size()) {
			throw new IllegalArgumentException("há clubes repetidos entre os inscritos da copa");
		}
		List<Clube> chave = new ArrayList<>(inscritos);
		Collections.shuffle(chave, aleatorio);
		List<ConfrontoCopa> confrontos = new ArrayList<>();
		for (int k = 0; k < chave.size() / 2; k++) {
			confrontos.add(confronto(primeira, k, chave.get(2 * k), chave.get(2 * k + 1), aleatorio));
		}
		return new Copa(confrontos);
	}

	private static ConfrontoCopa confronto(FaseCopa fase, int ordem, Clube a, Clube b, RandomGenerator aleatorio) {
		return aleatorio.nextBoolean() ? ConfrontoCopa.novo(fase, ordem, a, b) : ConfrontoCopa.novo(fase, ordem, b, a);
	}

	/**
	 * Etapas da copa em ordem, da primeira fase à final (SPEC §8.2, rev. 69).
	 *
	 * @param ultimaRodada a última rodada da liga, depois da qual se joga a final
	 * @throws IllegalArgumentException se a liga termina antes da volta da semifinal
	 */
	public static List<Etapa> quadro(FaseCopa primeira, int ultimaRodada, ParametrosCopa parametros) {
		List<Etapa> etapas = new ArrayList<>();
		for (FaseCopa fase = primeira; !fase.jogoUnico(); fase = fase.seguinte().orElseThrow()) {
			ParametrosCopa.Pernas pernas = parametros.pernas(fase);
			etapas.add(new Etapa(pernas.ida(), fase, Perna.IDA));
			etapas.add(new Etapa(pernas.volta(), fase, Perna.VOLTA));
		}
		if (!etapas.isEmpty() && etapas.getLast().depoisDaRodada() > ultimaRodada) {
			throw new IllegalArgumentException("a liga termina na rodada " + ultimaRodada + ", antes da "
					+ etapas.getLast().fase().nome() + " (depois da rodada " + etapas.getLast().depoisDaRodada() + ")");
		}
		etapas.add(new Etapa(ultimaRodada, FaseCopa.FINAL, Perna.JOGO_UNICO));
		return List.copyOf(etapas);
	}

	public FaseCopa primeiraFase() {
		return confrontos.getFirst().fase();
	}

	/** A fase mais adiantada já criada. */
	public FaseCopa faseAtual() {
		return confrontos.getLast().fase();
	}

	public List<ConfrontoCopa> confrontosDa(FaseCopa fase) {
		return confrontos.stream().filter(c -> c.fase() == fase).toList();
	}

	/** As partidas da etapa, se a fase dela é a atual e a perna ainda não foi jogada. */
	public List<PartidaCopa> partidas(Etapa etapa) {
		if (etapa.fase() != faseAtual()) {
			return List.of();
		}
		List<PartidaCopa> partidas = new ArrayList<>();
		for (ConfrontoCopa c : confrontosDa(etapa.fase())) {
			int rodada = etapa.depoisDaRodada();
			switch (etapa.perna()) {
				case IDA -> {
					if (c.ida().isEmpty()) {
						partidas.add(new PartidaCopa(c, Perna.IDA, new Jogo(rodada, c.mandante(), c.visitante()),
								Optional.empty()));
					}
				}
				case VOLTA -> {
					if (c.ida().isPresent() && c.volta().isEmpty()) {
						partidas.add(new PartidaCopa(c, Perna.VOLTA, new Jogo(rodada, c.visitante(), c.mandante()),
								Optional.of(c.decisivo())));
					}
				}
				case JOGO_UNICO -> {
					if (c.ida().isEmpty()) {
						partidas.add(new PartidaCopa(c, Perna.JOGO_UNICO, new Jogo(rodada, c.mandante(), c.visitante()),
								Optional.of(c.decisivo())));
					}
				}
			}
		}
		return partidas;
	}

	/**
	 * Registra o placar de uma partida.
	 *
	 * @param placar visto do mandante desta partida (na volta, o visitante da ida)
	 */
	public Copa comPlacar(FaseCopa fase, int ordem, Perna perna, PlacarCopa placar) {
		return trocar(fase, ordem, c -> switch (perna) {
			case IDA, JOGO_UNICO -> {
				if (perna == Perna.JOGO_UNICO != fase.jogoUnico()) {
					throw new IllegalArgumentException("a perna " + perna + " não existe na fase " + fase.nome());
				}
				yield c.comIda(placar);
			}
			case VOLTA -> c.comVolta(placar);
		});
	}

	private Copa trocar(FaseCopa fase, int ordem, UnaryOperator<ConfrontoCopa> troca) {
		List<ConfrontoCopa> novos = new ArrayList<>(confrontos);
		for (int i = 0; i < novos.size(); i++) {
			ConfrontoCopa c = novos.get(i);
			if (c.fase() == fase && c.ordem() == ordem) {
				novos.set(i, troca.apply(c));
				return new Copa(novos);
			}
		}
		throw new IllegalArgumentException("não há confronto " + ordem + " na fase " + fase.nome());
	}

	/** A fase atual está decidida e há uma seguinte a criar. */
	public boolean podeAvancar() {
		return !faseAtual().jogoUnico() && confrontosDa(faseAtual()).stream().allMatch(ConfrontoCopa::decidido);
	}

	/** Cria os confrontos da fase seguinte com os vencedores da atual, sorteando o mando da ida. */
	public Copa avancar(RandomGenerator aleatorio) {
		if (!podeAvancar()) {
			throw new IllegalStateException("a fase " + faseAtual().nome() + " ainda não terminou ou é a final");
		}
		List<ConfrontoCopa> atuais = confrontosDa(faseAtual());
		FaseCopa seguinte = faseAtual().seguinte().orElseThrow();
		List<ConfrontoCopa> novos = new ArrayList<>(confrontos);
		for (int k = 0; k < atuais.size() / 2; k++) {
			novos.add(confronto(seguinte, k, atuais.get(2 * k).vencedor().orElseThrow(),
					atuais.get(2 * k + 1).vencedor().orElseThrow(), aleatorio));
		}
		return new Copa(novos);
	}

	public Optional<Clube> campeao() {
		return finalDecidida().flatMap(ConfrontoCopa::vencedor);
	}

	public Optional<Clube> vice() {
		return finalDecidida().flatMap(ConfrontoCopa::perdedor);
	}

	private Optional<ConfrontoCopa> finalDecidida() {
		return faseAtual() == FaseCopa.FINAL ? Optional.of(confrontos.getLast()).filter(ConfrontoCopa::decidido)
				: Optional.empty();
	}

	/**
	 * Os jogos do clube nas etapas do quadro cujos confrontos já existem, na ordem do quadro, com o
	 * placar do ponto de vista dele. Vazio se ele não está na copa.
	 */
	public List<JogoDoClube> jogosDo(Clube clube, List<Etapa> quadro) {
		Objects.requireNonNull(clube);
		List<JogoDoClube> jogos = new ArrayList<>();
		for (int i = 0; i < quadro.size(); i++) {
			Etapa etapa = quadro.get(i);
			int indice = i;
			confrontosDa(etapa.fase()).stream().filter(c -> c.envolve(clube)).findFirst()
				.ifPresent(c -> jogos.add(jogoDo(c, etapa, indice, c.mandante().equals(clube))));
		}
		return jogos;
	}

	/** {@code primeiro}: o clube é o mandante da ida (o primeiro do confronto). */
	private static JogoDoClube jogoDo(ConfrontoCopa c, Etapa etapa, int indice, boolean primeiro) {
		Clube adversario = primeiro ? c.visitante() : c.mandante();
		// o placar de cada partida é visto de quem joga em casa nela; na volta, o segundo do confronto
		boolean emCasaNaPartida = etapa.perna() == Perna.VOLTA ? !primeiro : primeiro;
		Optional<PlacarCopa> placar = etapa.perna() == Perna.VOLTA ? c.volta() : c.ida();
		Integer pro = placar.map(p -> emCasaNaPartida ? p.golsMandante() : p.golsVisitante()).orElse(null);
		Integer contra = placar.map(p -> emCasaNaPartida ? p.golsVisitante() : p.golsMandante()).orElse(null);
		String penaltis = placar.flatMap(PlacarCopa::penaltis)
			.map(p -> "pên. " + (emCasaNaPartida ? p.mandante() : p.visitante()) + " x "
					+ (emCasaNaPartida ? p.visitante() : p.mandante()))
			.orElse(null);
		String detalhe = switch (etapa.perna()) {
			case IDA -> null;
			case JOGO_UNICO -> penaltis;
			case VOLTA -> {
				if (placar.isPresent()) {
					int somaPro = primeiro ? c.golsDoMandanteSomados() : c.golsDoVisitanteSomados();
					int somaContra = primeiro ? c.golsDoVisitanteSomados() : c.golsDoMandanteSomados();
					boolean passa = c.vencedor().orElseThrow().equals(primeiro ? c.mandante() : c.visitante());
					yield "total " + somaPro + " x " + somaContra + (penaltis == null ? "" : " · " + penaltis)
							+ (passa ? " · passa" : " · sai");
				}
				yield c.ida().map(p -> "ida " + (primeiro ? p.golsMandante() : p.golsVisitante()) + " x "
						+ (primeiro ? p.golsVisitante() : p.golsMandante())).orElse(null);
			}
		};
		return new JogoDoClube(indice, etapa, etapa.perna() == Perna.JOGO_UNICO ? null : emCasaNaPartida, adversario,
				pro, contra, detalhe);
	}

	/** O clube ainda está na copa: inscrito e sem ter sido eliminado. */
	public boolean naDisputa(Clube clube) {
		Objects.requireNonNull(clube);
		return confrontosDa(faseAtual()).stream()
			.anyMatch(c -> c.envolve(clube) && c.perdedor().map(p -> !p.equals(clube)).orElse(true));
	}
}
