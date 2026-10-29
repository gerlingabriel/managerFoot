package br.com.managerfoot.motor;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.random.RandomGenerator;

import br.com.managerfoot.dominio.Temperamento;

/**
 * Lesões (SPEC §7.5, rev. 69).
 *
 * @param lesoesPorMinuto chance de um time ter um lesionado em cada minuto
 * @param pesosTemperamento peso de cada temperamento no sorteio de quem se lesiona
 * @param gravidades faixa de cada gravidade
 * @param limitePorTemporada lesões no máximo por clube na temporada (definido pelo usuário: 2)
 */
public record ParametrosLesao(double lesoesPorMinuto, Map<Temperamento, Double> pesosTemperamento,
		Map<GravidadeLesao, Faixa> gravidades, int limitePorTemporada) {

	/**
	 * @param proporcao chance relativa desta gravidade entre as lesões
	 * @param jogosMinimo jogos fora, de {@code jogosMinimo} a {@code jogosMaximo}
	 * @param perdaMinima perda de força, de {@code perdaMinima} a {@code perdaMaxima}
	 */
	public record Faixa(double proporcao, int jogosMinimo, int jogosMaximo, int perdaMinima, int perdaMaxima) {

		public Faixa {
			if (proporcao < 0 || jogosMinimo <= 0 || jogosMaximo < jogosMinimo || perdaMinima < 0
					|| perdaMaxima < perdaMinima) {
				throw new IllegalArgumentException("faixa de lesão inválida");
			}
		}
	}

	public ParametrosLesao {
		if (lesoesPorMinuto < 0 || lesoesPorMinuto > 1) {
			throw new IllegalArgumentException("lesoesPorMinuto deve estar entre 0 e 1, mas foi " + lesoesPorMinuto);
		}
		if (limitePorTemporada < 0) {
			throw new IllegalArgumentException("limitePorTemporada não pode ser negativo");
		}
		pesosTemperamento = Map.copyOf(Objects.requireNonNull(pesosTemperamento));
		gravidades = Map.copyOf(Objects.requireNonNull(gravidades));
		for (Temperamento temperamento : Temperamento.values()) {
			Double peso = pesosTemperamento.get(temperamento);
			if (peso == null || peso <= 0) {
				throw new IllegalArgumentException("peso de lesão do temperamento " + temperamento + " deve ser positivo");
			}
		}
		for (GravidadeLesao gravidade : GravidadeLesao.values()) {
			if (!gravidades.containsKey(gravidade)) {
				throw new IllegalArgumentException("falta a faixa da lesão " + gravidade);
			}
		}
		if (gravidades.values().stream().mapToDouble(Faixa::proporcao).sum() <= 0) {
			throw new IllegalArgumentException("a soma das proporções das gravidades deve ser positiva");
		}
	}

	public double peso(Temperamento temperamento) {
		return pesosTemperamento.get(temperamento);
	}

	/** Sorteia a gravidade pela proporção e, dentro da faixa, os jogos fora e a perda de força. */
	Lesao sortear(RandomGenerator aleatorio) {
		GravidadeLesao gravidade = Sorteio.ponderado(List.of(GravidadeLesao.values()),
				g -> gravidades.get(g).proporcao(), aleatorio);
		Faixa faixa = gravidades.get(gravidade);
		return new Lesao(gravidade, aleatorio.nextInt(faixa.jogosMinimo(), faixa.jogosMaximo() + 1),
				aleatorio.nextInt(faixa.perdaMinima(), faixa.perdaMaxima() + 1));
	}

	/**
	 * Faixas e limite do usuário (rev. 69); frequência (~1,5 por clube por temporada de 38 rodadas,
	 * antes do limite), proporção 60/30/10 e pesos 5/4/3/2/1 são provisórios do agente.
	 */
	public static ParametrosLesao padrao() {
		Map<Temperamento, Double> pesos = new EnumMap<>(Temperamento.class);
		pesos.put(Temperamento.CALMO, 5.0);
		pesos.put(Temperamento.EDUCADO, 4.0);
		pesos.put(Temperamento.NORMAL, 3.0);
		pesos.put(Temperamento.ANSIOSO, 2.0);
		pesos.put(Temperamento.VIOLENTO, 1.0);
		Map<GravidadeLesao, Faixa> gravidades = new EnumMap<>(GravidadeLesao.class);
		gravidades.put(GravidadeLesao.LEVE, new Faixa(0.6, 1, 3, 1, 3));
		gravidades.put(GravidadeLesao.MEDIA, new Faixa(0.3, 3, 6, 3, 6));
		gravidades.put(GravidadeLesao.GRAVE, new Faixa(0.1, 5, 9, 5, 9));
		return new ParametrosLesao(1.5 / (38 * 90), pesos, gravidades, 2);
	}
}
