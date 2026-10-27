package br.com.managerfoot.temporada;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SplittableRandom;

import br.com.managerfoot.dominio.Clube;

/**
 * Tabela de pontos corridos (SPEC §8.1). Vitória vale 3, empate 1. Ordem: pontos, mais vitórias,
 * menos derrotas, menos empates, saldo, gols marcados, menos gols sofridos e sorteio. Os critérios
 * valem para todas as posições.
 *
 * <p>O sorteio é fixo na temporada: cada clube recebe um número derivado da semente da temporada
 * e do seu id, então recalcular a tabela não troca a ordem de clubes empatados.
 */
public record Classificacao(List<LinhaClassificacao> linhas) {

	public static final int PONTOS_VITORIA = 3;
	public static final int PONTOS_EMPATE = 1;

	public Classificacao {
		linhas = List.copyOf(linhas);
	}

	/**
	 * @param resultados resultados já disputados; um mesmo jogo não pode aparecer duas vezes
	 * @param sementeSorteio semente da temporada para o último critério de desempate
	 */
	public static Classificacao calcular(List<Clube> clubes, List<ResultadoJogo> resultados, long sementeSorteio) {
		Objects.requireNonNull(clubes, "lista de clubes é obrigatória");
		Objects.requireNonNull(resultados, "lista de resultados é obrigatória");
		Map<Long, LinhaClassificacao> porClube = new LinkedHashMap<>();
		for (Clube clube : clubes) {
			porClube.put(clube.id(), new LinhaClassificacao(clube, 0, 0, 0, 0, 0, 0));
		}

		Set<Jogo> processados = new HashSet<>();
		for (ResultadoJogo resultado : resultados) {
			Jogo jogo = resultado.jogo();
			if (!processados.add(jogo)) {
				throw new IllegalArgumentException("o jogo da rodada " + jogo.rodada() + " " + jogo.mandante().nome()
						+ " x " + jogo.visitante().nome() + " foi informado mais de uma vez");
			}
			atualizar(porClube, jogo.mandante(), resultado.golsMandante(), resultado.golsVisitante());
			atualizar(porClube, jogo.visitante(), resultado.golsVisitante(), resultado.golsMandante());
		}

		Comparator<LinhaClassificacao> ordem = Comparator.comparingInt(LinhaClassificacao::pontos).reversed()
			.thenComparing(Comparator.comparingInt(LinhaClassificacao::vitorias).reversed())
			.thenComparingInt(LinhaClassificacao::derrotas)
			.thenComparingInt(LinhaClassificacao::empates)
			.thenComparing(Comparator.comparingInt(LinhaClassificacao::saldo).reversed())
			.thenComparing(Comparator.comparingInt(LinhaClassificacao::golsPro).reversed())
			.thenComparingInt(LinhaClassificacao::golsContra)
			.thenComparingLong(l -> sorteio(sementeSorteio, l.clube()));
		return new Classificacao(porClube.values().stream().sorted(ordem).toList());
	}

	private static void atualizar(Map<Long, LinhaClassificacao> porClube, Clube clube, int marcados, int sofridos) {
		LinhaClassificacao linha = porClube.get(clube.id());
		if (linha == null) {
			throw new IllegalArgumentException(clube.nome() + " tem resultado, mas não faz parte desta classificação");
		}
		porClube.put(clube.id(), linha.comResultado(marcados, sofridos));
	}

	/** Número fixo por clube na temporada; o menor fica à frente no último critério. */
	private static long sorteio(long semente, Clube clube) {
		return new SplittableRandom(semente ^ (clube.id() * 0x9E3779B97F4A7C15L)).nextLong();
	}

	/** Posição do clube, começando em 1. */
	public int posicao(Clube clube) {
		for (int i = 0; i < linhas.size(); i++) {
			if (linhas.get(i).clube().equals(clube)) {
				return i + 1;
			}
		}
		throw new IllegalArgumentException(clube.nome() + " não está nesta classificação");
	}

	public LinhaClassificacao linha(Clube clube) {
		return linhas.get(posicao(clube) - 1);
	}
}
