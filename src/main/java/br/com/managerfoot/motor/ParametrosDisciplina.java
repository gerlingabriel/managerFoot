package br.com.managerfoot.motor;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import br.com.managerfoot.dominio.Temperamento;

/**
 * Parâmetros de cartões (SPEC §7.4). Valores padrão provisórios, calibrados na Etapa 1d
 * (metas da §7.6: ~2 amarelos e 0,1–0,2 expulsão por jogo).
 *
 * @param cartoesPorMinuto chance de cartão por minuto para um time só de jogadores "normais"
 * @param proporcaoVermelhoDireto fração dos cartões que é vermelho direto
 * @param sensibilidadeTemperamento expoente aplicado à soma dos pesos do time em relação a um time
 *        só de "normais"; 0 = temperamento não muda a frequência, 1 = proporcional
 * @param pesosTemperamento peso de cada temperamento na roleta de quem recebe o cartão
 */
public record ParametrosDisciplina(double cartoesPorMinuto, double proporcaoVermelhoDireto,
		double sensibilidadeTemperamento, Map<Temperamento, Double> pesosTemperamento) {

	public ParametrosDisciplina {
		Objects.requireNonNull(pesosTemperamento, "pesos de temperamento são obrigatórios");
		pesosTemperamento = Map.copyOf(pesosTemperamento);
		for (Temperamento temperamento : Temperamento.values()) {
			Double peso = pesosTemperamento.get(temperamento);
			if (peso == null || peso <= 0) {
				throw new IllegalArgumentException("peso do temperamento " + temperamento + " deve ser positivo");
			}
		}
		if (cartoesPorMinuto < 0 || cartoesPorMinuto > 1 || proporcaoVermelhoDireto < 0 || proporcaoVermelhoDireto > 1) {
			throw new IllegalArgumentException("cartoesPorMinuto e proporcaoVermelhoDireto devem estar entre 0 e 1");
		}
		if (sensibilidadeTemperamento < 0) {
			throw new IllegalArgumentException("sensibilidadeTemperamento não pode ser negativa");
		}
	}

	public double peso(Temperamento temperamento) {
		return pesosTemperamento.get(temperamento);
	}

	/** Pesos 1/2/4/6/8 aprovados na SPEC §7.4. */
	public static ParametrosDisciplina padrao() {
		Map<Temperamento, Double> pesos = new EnumMap<>(Temperamento.class);
		pesos.put(Temperamento.CALMO, 1.0);
		pesos.put(Temperamento.EDUCADO, 2.0);
		pesos.put(Temperamento.NORMAL, 4.0);
		pesos.put(Temperamento.ANSIOSO, 6.0);
		pesos.put(Temperamento.VIOLENTO, 8.0);
		return new ParametrosDisciplina(2.15 / 90 / 2, 0.05, 0.25, pesos);
	}
}
