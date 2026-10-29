package br.com.managerfoot.dominio;

/**
 * Valores da moral (SPEC §6.2). Provisórios: devem ser validados por simulação de temporadas.
 *
 * @param inicial moral ao assumir um time
 * @param limiteVerde a partir deste valor a faixa é verde
 * @param limiteAmarelo a partir deste valor (e abaixo do verde) a faixa é amarela; abaixo, vermelha
 * @param divisorRetorno a cada jogo a moral volta (inicial − atual) / divisor em direção à inicial,
 *        para não saturar nos extremos; 0 desliga
 * @param diferencaPorPonto a cada tantos pontos de força a mais do adversário (força em campo), o jogo
 *        vale 1 ponto de moral a mais; a menos, 1 a menos (rev. 61: o forte que perde cai mais, o fraco
 *        que ganha sobe mais); 0 desliga
 * @param ajusteMaximo limite desse ajuste, para mais ou para menos
 */
public record ParametrosMoral(int minimo, int maximo, int inicial, int vitoriaEmCasa, int vitoriaFora,
		int empateEmCasa, int empateFora, int derrotaEmCasa, int derrotaFora, int limiteVerde, int limiteAmarelo,
		int divisorRetorno, double diferencaPorPonto, int ajusteMaximo) {

	/** Sem ajuste pela força do adversário. */
	public ParametrosMoral(int minimo, int maximo, int inicial, int vitoriaEmCasa, int vitoriaFora, int empateEmCasa,
			int empateFora, int derrotaEmCasa, int derrotaFora, int limiteVerde, int limiteAmarelo, int divisorRetorno) {
		this(minimo, maximo, inicial, vitoriaEmCasa, vitoriaFora, empateEmCasa, empateFora, derrotaEmCasa, derrotaFora,
				limiteVerde, limiteAmarelo, divisorRetorno, 0, 0);
	}

	public ParametrosMoral {
		if (minimo >= maximo || inicial < minimo || inicial > maximo) {
			throw new IllegalArgumentException("a moral inicial deve estar entre o mínimo e o máximo");
		}
		if (!(minimo < limiteAmarelo && limiteAmarelo < limiteVerde && limiteVerde <= maximo)) {
			throw new IllegalArgumentException("as faixas devem respeitar mínimo < amarelo < verde <= máximo");
		}
		if (divisorRetorno < 0) {
			throw new IllegalArgumentException("divisorRetorno não pode ser negativo");
		}
		if (diferencaPorPonto < 0 || ajusteMaximo < 0) {
			throw new IllegalArgumentException("diferencaPorPonto e ajusteMaximo não podem ser negativos");
		}
	}

	/**
	 * Escala 0-30, início 22, faixas verde 15-30 / amarelo 7-14 / vermelho 0-6 (rev. 61). Proposta da rev.
	 * 61, simulada em {@code docs/simulacoes/demissao.md}: retorno à inicial com divisor 20 (era 12) e a
	 * surpresa vale 1 ponto a cada 3 de diferença de força do adversário, até 3.
	 */
	public static ParametrosMoral padrao() {
		return new ParametrosMoral(0, 30, 22, 2, 3, -1, 0, -3, -2, 15, 7, 20, 3, 3);
	}
}
