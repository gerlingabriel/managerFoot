package br.com.managerfoot.temporada;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SplittableRandom;

import br.com.managerfoot.dominio.Clube;

/**
 * Um grupo da competição continental (SPEC §8.3.6): turno e returno, desempate por pontos, saldo,
 * gols pró e sorteio (rev. 72).
 *
 * @param indice 0 é o grupo A
 * @param clubes na ordem dos potes: o primeiro veio do pote 1
 * @param partidas os jogos do grupo, do 1º ao último
 */
public record GrupoContinental(int indice, List<Clube> clubes, List<PartidaGrupo> partidas) {

	/**
	 * Uma partida do grupo.
	 *
	 * @param jogo de 1 ao número de jogos do grupo
	 * @param placar gols do mandante e do visitante, quando jogada
	 */
	public record PartidaGrupo(int jogo, Clube mandante, Clube visitante, Optional<Placar> placar) {

		public PartidaGrupo {
			Objects.requireNonNull(mandante);
			Objects.requireNonNull(visitante);
			Objects.requireNonNull(placar);
		}

		public boolean envolve(Clube clube) {
			return mandante.equals(clube) || visitante.equals(clube);
		}
	}

	public record Placar(int golsMandante, int golsVisitante) {

		public Placar {
			if (golsMandante < 0 || golsVisitante < 0) {
				throw new IllegalArgumentException("gols não podem ser negativos: " + golsMandante + "x" + golsVisitante);
			}
		}
	}

	/** Turno de 4 clubes: cada par uma vez, com 2 jogos em casa para os dois primeiros e 1 para os outros. */
	private static final int[][][] TURNO_DE_QUATRO = { { { 0, 1 }, { 2, 3 } }, { { 3, 0 }, { 1, 2 } },
			{ { 0, 2 }, { 3, 1 } } };

	public GrupoContinental {
		clubes = List.copyOf(clubes);
		partidas = List.copyOf(partidas);
		if (clubes.stream().distinct().count() != clubes.size()) {
			throw new IllegalArgumentException("há clubes repetidos no grupo " + letra(indice));
		}
	}

	/** Grupo novo com a tabela de turno e returno; o returno inverte o mando do turno. */
	public static GrupoContinental novo(int indice, List<Clube> clubes) {
		if (clubes.size() != 4) {
			throw new IllegalArgumentException("o grupo tem 4 clubes, mas recebeu " + clubes.size());
		}
		List<PartidaGrupo> partidas = new ArrayList<>();
		for (int[][] rodada : TURNO_DE_QUATRO) {
			for (int[] par : rodada) {
				partidas.add(new PartidaGrupo(partidas.size() / 2 + 1, clubes.get(par[0]), clubes.get(par[1]),
						Optional.empty()));
			}
		}
		int turno = partidas.size();
		for (int i = 0; i < turno; i++) {
			PartidaGrupo ida = partidas.get(i);
			partidas.add(new PartidaGrupo(ida.jogo() + TURNO_DE_QUATRO.length, ida.visitante(), ida.mandante(),
					Optional.empty()));
		}
		return new GrupoContinental(indice, clubes, partidas);
	}

	public static String letra(int indice) {
		return String.valueOf((char) ('A' + indice));
	}

	public String nome() {
		return "Grupo " + letra(indice);
	}

	public int jogos() {
		return partidas.getLast().jogo();
	}

	public List<PartidaGrupo> partidasDoJogo(int jogo) {
		return partidas.stream().filter(p -> p.jogo() == jogo).toList();
	}

	public GrupoContinental comPlacar(int jogo, Clube mandante, Placar placar) {
		List<PartidaGrupo> novas = new ArrayList<>(partidas);
		for (int i = 0; i < novas.size(); i++) {
			PartidaGrupo p = novas.get(i);
			if (p.jogo() == jogo && p.mandante().equals(mandante)) {
				if (p.placar().isPresent()) {
					throw new IllegalStateException(p.mandante().nome() + " x " + p.visitante().nome() + " já foi jogado");
				}
				novas.set(i, new PartidaGrupo(jogo, p.mandante(), p.visitante(), Optional.of(placar)));
				return new GrupoContinental(indice, clubes, novas);
			}
		}
		throw new IllegalArgumentException(mandante.nome() + " não joga em casa no jogo " + jogo + " do " + nome());
	}

	public boolean encerrado() {
		return partidas.stream().allMatch(p -> p.placar().isPresent());
	}

	/**
	 * Tabela do grupo: pontos, saldo, gols pró e sorteio fixo na temporada (rev. 72).
	 *
	 * @param sementeSorteio a mesma semente dá a mesma ordem entre empatados em tudo
	 */
	public List<LinhaClassificacao> classificacao(long sementeSorteio) {
		Map<Clube, LinhaClassificacao> linhas = new LinkedHashMap<>();
		clubes.forEach(c -> linhas.put(c, new LinhaClassificacao(c, 0, 0, 0, 0, 0, 0)));
		for (PartidaGrupo p : partidas) {
			p.placar().ifPresent(placar -> {
				linhas.computeIfPresent(p.mandante(), (c, l) -> l.comResultado(placar.golsMandante(), placar.golsVisitante()));
				linhas.computeIfPresent(p.visitante(), (c, l) -> l.comResultado(placar.golsVisitante(), placar.golsMandante()));
			});
		}
		Comparator<LinhaClassificacao> ordem = Comparator.comparingInt(LinhaClassificacao::pontos).reversed()
			.thenComparing(Comparator.comparingInt(LinhaClassificacao::saldo).reversed())
			.thenComparing(Comparator.comparingInt(LinhaClassificacao::golsPro).reversed())
			.thenComparingLong(l -> new SplittableRandom(sementeSorteio ^ (l.clube().id() * 0x9E3779B97F4A7C15L)).nextLong());
		return linhas.values().stream().sorted(ordem).toList();
	}

	/** O 1º e o 2º do grupo encerrado. */
	public List<Clube> classificados(long sementeSorteio) {
		if (!encerrado()) {
			throw new IllegalStateException("o " + nome() + " ainda tem jogos a disputar");
		}
		return classificacao(sementeSorteio).stream()
			.limit(ParametrosContinental.CLASSIFICADOS_POR_GRUPO)
			.map(LinhaClassificacao::clube)
			.toList();
	}
}
