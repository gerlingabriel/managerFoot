package br.com.managerfoot.temporada;

import java.util.EnumMap;
import java.util.Map;

/**
 * Parâmetros da evolução de força depois de cada partida (SPEC §6.1). Proposta do agente a partir da
 * descrição do usuário (rev. 54), simulada em {@code docs/simulacoes/evolucao-forca.md}.
 *
 * @param perdaDeQuemJogou quantos dos que jogaram perdem 1 ponto, em média, por resultado (a chance de
 *        cada um é esse número dividido por 11)
 * @param crescimentoNoBanco chance de +1 de cada reserva que não entrou, por resultado
 * @param crescimentoFora chance de +1 de cada jogador fora da escalação, por resultado
 * @param perdaAcimaDoTeto chance de −1 por partida de quem está acima do teto da divisão, além da
 *        perda normal; acima do teto ninguém cresce
 * @param abaixoDoClube o desgaste de quem jogou não leva abaixo da força do clube menos este número;
 *        {@link Integer#MAX_VALUE} = sem piso
 */
public record ParametrosEvolucao(Map<Desfecho, Double> perdaDeQuemJogou, Map<Desfecho, Double> crescimentoNoBanco,
		Map<Desfecho, Double> crescimentoFora, double perdaAcimaDoTeto, int abaixoDoClube) {

	/** Sem piso pela força do clube. */
	public static final int SEM_PISO = Integer.MAX_VALUE;

	/** Titulares em campo: a perda média é dividida por eles. */
	public static final int EM_CAMPO = 11;

	public ParametrosEvolucao {
		perdaDeQuemJogou = completo(perdaDeQuemJogou, "perdaDeQuemJogou", EM_CAMPO);
		crescimentoNoBanco = completo(crescimentoNoBanco, "crescimentoNoBanco", 1);
		crescimentoFora = completo(crescimentoFora, "crescimentoFora", 1);
		if (abaixoDoClube < 0) {
			throw new IllegalArgumentException("abaixoDoClube não pode ser negativo: " + abaixoDoClube);
		}
		if (perdaAcimaDoTeto < 0 || perdaAcimaDoTeto > 1) {
			throw new IllegalArgumentException("perdaAcimaDoTeto deve estar entre 0 e 1, mas foi " + perdaAcimaDoTeto);
		}
	}

	/**
	 * Leitura literal da descrição do usuário (rev. 54): dos que jogaram, perdem 1 ponto cerca de 1 na
	 * vitória, 3 no empate e 5 na derrota; quem não jogou cresce só na vitória (às vezes), devagar no
	 * empate e quase nada na derrota.
	 */
	public static ParametrosEvolucao literal() {
		return new ParametrosEvolucao(porDesfecho(1, 3, 5), porDesfecho(0.15, 0.08, 0.015), porDesfecho(0.15, 0.08, 0.015),
				0.25, SEM_PISO);
	}

	/**
	 * Proposta do agente (variante E do relatório): a mesma perda, crescimento 2,5 vezes o da leitura
	 * literal e piso na força do clube − 5. Sem piso, o clube que perde entra em espiral até força 1;
	 * com o crescimento literal, todas as divisões derretem.
	 */
	public static ParametrosEvolucao padrao() {
		return new ParametrosEvolucao(porDesfecho(1, 3, 5), porDesfecho(0.375, 0.20, 0.0375), porDesfecho(0.375, 0.20, 0.0375),
				0.25, 5);
	}

	public static Map<Desfecho, Double> porDesfecho(double vitoria, double empate, double derrota) {
		Map<Desfecho, Double> m = new EnumMap<>(Desfecho.class);
		m.put(Desfecho.VITORIA, vitoria);
		m.put(Desfecho.EMPATE, empate);
		m.put(Desfecho.DERROTA, derrota);
		return m;
	}

	private static Map<Desfecho, Double> completo(Map<Desfecho, Double> valores, String nome, double maximo) {
		for (Desfecho d : Desfecho.values()) {
			Double v = valores.get(d);
			if (v == null || v < 0 || v > maximo) {
				throw new IllegalArgumentException(nome + " de " + d + " deve estar entre 0 e " + maximo + ", mas foi " + v);
			}
		}
		return Map.copyOf(valores);
	}

	/** Resultado da partida para o clube. */
	public enum Desfecho {
		VITORIA, EMPATE, DERROTA;

		public static Desfecho de(int golsPro, int golsContra) {
			return golsPro > golsContra ? VITORIA : golsPro == golsContra ? EMPATE : DERROTA;
		}
	}
}
