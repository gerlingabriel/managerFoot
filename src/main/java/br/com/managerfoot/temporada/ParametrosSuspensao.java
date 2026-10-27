package br.com.managerfoot.temporada;

import java.util.List;

/**
 * Suspensões por cartões (SPEC §7.4).
 *
 * @param amarelosParaSuspensao amarelos acumulados na temporada que suspendem 1 jogo
 * @param pesosVermelhoDireto peso do sorteio da suspensão por vermelho direto; o índice 0 é 1 jogo,
 *        o 1 é 2 jogos e assim por diante
 */
public record ParametrosSuspensao(int amarelosParaSuspensao, List<Integer> pesosVermelhoDireto) {

	public ParametrosSuspensao {
		if (amarelosParaSuspensao < 1) {
			throw new IllegalArgumentException("amarelosParaSuspensao deve ser ao menos 1, mas foi " + amarelosParaSuspensao);
		}
		pesosVermelhoDireto = List.copyOf(pesosVermelhoDireto);
		if (pesosVermelhoDireto.isEmpty() || pesosVermelhoDireto.stream().anyMatch(p -> p < 0)
				|| pesosVermelhoDireto.stream().mapToInt(Integer::intValue).sum() == 0) {
			throw new IllegalArgumentException("os pesos do vermelho direto devem ser não negativos e somar mais que zero");
		}
	}

	/** 5 amarelos; vermelho direto: 1 jogo em 70%, 2 em 25% e 3 em 5% (SPEC rev. 22, provisório). */
	public static ParametrosSuspensao padrao() {
		return new ParametrosSuspensao(5, List.of(70, 25, 5));
	}
}
