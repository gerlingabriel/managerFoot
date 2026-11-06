package br.com.managerfoot.temporada;

import java.util.List;
import java.util.Optional;

/**
 * Bônus do treinador usuário (SPEC §5.3.2, rev. 121): pela força em campo do clube na hora do sorteio, alguns
 * jogadores ganham o asterisco temporário e força extra em campo até o fim da temporada.
 *
 * @param faixas da mais fraca para a mais forte; acima da última, nada
 */
public record ParametrosBonusTreinador(List<Faixa> faixas) {

	/**
	 * @param forcaAte força em campo máxima da faixa (arredondada)
	 * @param jogadores quantos são sorteados
	 * @param forcaExtra quanto cada um ganha em campo
	 */
	public record Faixa(int forcaAte, int jogadores, int forcaExtra) {

		public Faixa {
			if (jogadores < 0 || forcaExtra < 0) {
				throw new IllegalArgumentException("faixa do bônus do treinador inválida: " + jogadores + " jogadores, +" + forcaExtra);
			}
		}
	}

	public ParametrosBonusTreinador {
		faixas = List.copyOf(faixas);
		for (int i = 1; i < faixas.size(); i++) {
			if (faixas.get(i).forcaAte() <= faixas.get(i - 1).forcaAte()) {
				throw new IllegalArgumentException("as faixas do bônus do treinador devem estar em ordem de força");
			}
		}
	}

	/** Até 30: 3 jogadores com +10; de 31 a 40: 2 com +5 (pelo usuário, rev. 121). */
	public static ParametrosBonusTreinador padrao() {
		return new ParametrosBonusTreinador(List.of(new Faixa(30, 3, 10), new Faixa(40, 2, 5)));
	}

	/** A faixa da força em campo dada, arredondada; vazia acima da última. */
	public Optional<Faixa> faixa(double forcaEmCampo) {
		long forca = Math.round(forcaEmCampo);
		return faixas.stream().filter(f -> forca <= f.forcaAte()).findFirst();
	}
}
