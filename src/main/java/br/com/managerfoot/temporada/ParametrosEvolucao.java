package br.com.managerfoot.temporada;

import java.util.EnumMap;
import java.util.Map;

/**
 * Parâmetros da evolução de força depois de cada partida (SPEC §6.1, rev. 54 e 55). Cada jogador
 * muda no máximo 1 ponto por partida; o resultado decide quantos mudam. Proposta do agente a partir
 * da descrição do usuário, simulada em {@code docs/simulacoes/evolucao-forca.md}.
 *
 * @param perdem quantos dos que jogaram perdem 1 ponto, por resultado (parte fracionária = chance de
 *        mais um)
 * @param crescem quantos dos que não jogaram (banco sem entrar e fora da escalação) ganham 1 ponto, por
 *        resultado (parte fracionária = chance de mais um)
 * @param perdaAcimaDoTeto chance de −1 por partida de quem está acima do teto da divisão; acima do teto
 *        ninguém cresce
 * @param abaixoDoClube o desgaste não leva abaixo da força do clube menos este número;
 *        {@link #SEM_PISO} = sem piso
 */
public record ParametrosEvolucao(Map<Desfecho, Double> perdem, Map<Desfecho, Double> crescem, double perdaAcimaDoTeto,
		int abaixoDoClube) {

	/** Sem piso pela força do clube. */
	public static final int SEM_PISO = Integer.MAX_VALUE;

	public ParametrosEvolucao {
		perdem = completo(perdem, "perdem");
		crescem = completo(crescem, "crescem");
		if (abaixoDoClube < 0) {
			throw new IllegalArgumentException("abaixoDoClube não pode ser negativo: " + abaixoDoClube);
		}
		if (perdaAcimaDoTeto < 0 || perdaAcimaDoTeto > 1) {
			throw new IllegalArgumentException("perdaAcimaDoTeto deve estar entre 0 e 1, mas foi " + perdaAcimaDoTeto);
		}
	}

	/**
	 * Leitura literal da descrição do usuário (rev. 55): dos que jogaram, perdem 1 ponto 1 jogador na
	 * vitória, 3 no empate e 5 na derrota; dos que não jogaram, crescem 2 na vitória, 1 no empate e
	 * nenhum na derrota.
	 */
	public static ParametrosEvolucao literal() {
		return new ParametrosEvolucao(porDesfecho(1, 3, 5), porDesfecho(2, 1, 0), 0.25, SEM_PISO);
	}

	/**
	 * Proposta do agente (variante C do relatório): crescem 4 na vitória, 2 no empate e 0,5 na derrota,
	 * com piso na força do clube − 5. Sem piso, o clube que perde desce sem parar; crescendo 5/3/1, um
	 * terço dos jogadores encosta no teto em 6 temporadas.
	 */
	public static ParametrosEvolucao padrao() {
		return new ParametrosEvolucao(porDesfecho(1, 3, 5), porDesfecho(4, 2, 0.5), 0.25, 5);
	}

	public static Map<Desfecho, Double> porDesfecho(double vitoria, double empate, double derrota) {
		Map<Desfecho, Double> m = new EnumMap<>(Desfecho.class);
		m.put(Desfecho.VITORIA, vitoria);
		m.put(Desfecho.EMPATE, empate);
		m.put(Desfecho.DERROTA, derrota);
		return m;
	}

	private static Map<Desfecho, Double> completo(Map<Desfecho, Double> valores, String nome) {
		for (Desfecho d : Desfecho.values()) {
			Double v = valores.get(d);
			if (v == null || v < 0) {
				throw new IllegalArgumentException(nome + " de " + d + " não pode ser negativo nem faltar: " + v);
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
